(ns slothcfg.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil]
            [leiningen.core.project :as project]
            leiningen.core.main
            leiningen.show-profiles
            [robert.hooke :refer [add-hook]])
  (:import java.util.Properties))

(def ^:dynamic *default-extension* ".clj")

(def ext-template-map
  {".clj" "templates/slothcfg-clj"
   ".cljx" "templates/slothcfg-cljx"
   ".cljs" "templates/slothcfg-cljs"})

;;
;; Functions that provide functionality independent of any one build tool.
;;

(defn read-current-profiles
  "Read the current profiles from the config file for the project
   at the given path. For example, if the argument is \"/home/david\", reads
   \"/home/david/.slothcfg/current\". .slothcfg/current should contain a
   single form that lists the currently active profiles."
  [config-store-path]
  (let [config-file (io/file config-store-path ".slothcfg/current")]
    (when (and (.exists config-file)
               (.isFile config-file))
      (binding [*read-eval* false]
        (read-string (slurp config-file))))))

(defn save-current-profiles
  "Store the given profiles as the new current profiles for the project
   at the given path."
  [config-store-path current-profiles]
  (let [config-dir (io/file config-store-path ".slothcfg")
        config-file (io/file config-store-path ".slothcfg/current")]
    (if (not (and (.exists config-dir)
                  (.isDirectory config-dir)))
      (.mkdir config-dir))
    (spit config-file (prn-str current-profiles))))

(defn get-current-profiles
  "Return the current profiles (list of names) from the saved file.
   Otherwise, return nil."
  []
  (read-current-profiles "."))

(defn- fixed-point
  "Calls the function argument on the initial value, and then on the
   result of function call, and so on, until the output value does not
   change. You should be sure that func is a contraction mapping, or it
   will loop forever."
  [func initial]
  (loop [input initial]
    (let [output (func input)]
      (if (= input output) output (recur output)))))

(defn expand-profile
  "Given a project map and a profile in the project map, expands the profile
   into a seq of its constituent profiles. Composite profiles are expanded one
   step, basic profiles are returned as the only member of the sequence."
  [project profile]
  (let [profile-val (get-in project [:profiles profile])]
    (if (vector? profile-val)
      profile-val
      [profile])))

(defn fully-expand-profile
  "Reduce a given profile into a set of the names of its basic profiles."
  [project profile]
  (fixed-point (fn [profiles]
                 (->> (map (partial expand-profile project) profiles)
                      (flatten)
                      ;; Append the flattened list of generated profiles onto
                      ;; a list of all the profiles we've found so far that are
                      ;; not composite (We just expanded the ones that are).
                      (concat (filter #(not (vector? (get-in project
                                                             [:profiles %])))
                                      profiles))
                      (into #{})))
               [profile]))

(defn unstickable-profiles
  "Return all of the sets of the name keys of basic profiles that
   cannot be made sticky according to the :slothcfg configuration
   key. Composite profiles are expanded away. Returns a set of the
   sets of basic keys that cannot be set sticky in combination."
   [project]
  (let [unstickable-profiles (into #{}
                                   (get-in project
                                           [:slothcfg :never-sticky]))]
    (into #{} (map (partial fully-expand-profile project)
                   unstickable-profiles))))

(defn print-current-sticky-profiles
  "Given a set of profiles as an argument, print them in a standard way
   to stdout."
  [profiles]
  (if (not (empty? profiles))
    (println "Current sticky profiles:" profiles)
    (println "Current sticky profiles:")))

(defn valid-profile?
  "Returns true if profile is a profile listed in the project profiles."
  [project profile]
  (contains? (apply hash-set (keys (:profiles project)))
             profile))

(defn config-namespace
  "Get the namespace to put the profile info into, or use the default
   (which is cfg.current)."
  [settings]
  (:namespace settings 'cfg.current))

(defn config-var
  "Get the var to put the profile info into, or use the default
   (which is project)."
  [project]
  (or (get-in project [:slothcfg :var])
      'project))

(defn merge-profiles
  "Given the project map and a list of profile names, merge the profiles into
   the project map and return the result. Will check the :included-profiles
   metadata key to ensure that profiles are not merged multiple times."
  [project profiles]
  (let [included-profiles (into #{} (:included-profiles (meta project)))
        profiles-to-add (filter #(and (not (contains? included-profiles %))
                                      (contains? (:profiles project) %))
                                profiles)]
    (project/merge-profiles project profiles-to-add)))

(defn fix-extension [ext]
  (if (.startsWith ext ".") ext (str "." ext)))

(defn namespace-to-filepath
  "Given a namespace as a string/symbol, return a string containing the path
   where we'd look for its source (with the appropriate file extension)."
  [ns-str ext]
  (str (string/replace ns-str #"[.-]" {"." "/", "-" "_"}) ext))

(defn sub-project
  "Return a nested subset of the project map. Also get the same nested subset from each value
  in :profiles and put that in the :profiles at the top level. If there is :without-profiles meta on
  project, do the same magic for it."
  [project keyseq]
  (when project
    (-> (get-in project keyseq)
        (assoc :profiles (into {}
                               (for [[profile config] (:profiles project)]
                                 [profile (get-in config keyseq)])))
        (with-meta (update-in (meta project)
                              [:without-profiles] sub-project keyseq)))))

(defn ext->template [ext]
  (if-let [tpl (ext-template-map ext)]
    tpl
    (do
      (println "WARNING: No custom :template provided and unknown :file-ext '"
               ext "'! Assuming '" *default-extension* "' Please fix your :slothcfg!")
      (ext-template-map *default-extension*))))

(defn output-config-namespace
  "Write a Clojure file that will set up the config namespace with the project
   in it when it is loaded. Returns the project with the profiles merged."
  [project]
  (let [settings (:slothcfg project)
        ns-str (str (config-namespace settings))
        src-path (or (:config-source-path settings)
                     (first (:source-paths project))
                     "src/")
        file-ext (fix-extension (:file-ext settings *default-extension*))
        template (:template settings (ext->template file-ext))
        ns-file (io/file src-path (namespace-to-filepath ns-str file-ext))
        ns-parent-dir (.getParentFile ns-file)
        config (if-let [keyseq (:keyseq settings)]
                 (sub-project project keyseq)
                 project)
        config (if-let [f (:middleware settings)]
                 ((eval f) config)
                 config)
        ; fix a bug caused by an unreadable form due to Leiningen 2.3.0:
        ; :compile-path #<classpath$checkout_deps_paths leiningen.core.classpath$checkout_deps_paths@41dee0d7>
        config (dissoc config :checkout-deps-shares)
        ]
    (if (not (.exists ns-parent-dir))
      (.mkdirs ns-parent-dir))
    (spit ns-file
          (stencil/render-file
           template
           {:namespace ns-str
            :var (config-var project)
            :config config
            :config-metadata (select-keys (meta config)
                                          [:without-profiles :included-profiles])}))))

(defn check-gitignore
  "Check the .gitignore file for the project to see if .slothcfg/ is ignored,
   and warn if it isn't. Also check for the config-namespace source file."
  [{settings :slothcfg}]
  (try
    (let [gitignore-file (io/file ".gitignore")
          gitignore-lines (string/split-lines (slurp gitignore-file))
          ns-str (config-namespace settings)
          ; characters between \Q and \E are interpreted as literal characters
          config-ns-re (re-pattern (str "\\Qsrc/" ns-str "\\E"))]
      (when (not (some #(re-matches #"\.slothcfg" %) gitignore-lines))
        (println "slothcfg")
        (println "  The .slothcfg directory does not appear to be present in")
        (println "  .gitignore. You almost certainly want to add it."))
      (when (not (some #(re-matches config-ns-re %) gitignore-lines))
        (println "slothcfg")
        (println "  The slothcfg namespace file," (str "src/" ns-str)
                 ", does not")
        (println "  appear to be present in .gitignore. You almost certainly")
        (println "  want to add it.")))
    ;; For now, just be quiet if we can't open the file.
    (catch Exception e)))
