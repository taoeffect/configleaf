(ns slothcfg.plugin
  (:require [slothcfg.core :refer [merge-profiles
                                      output-config-namespace
                                      print-current-sticky-profiles
                                      get-current-profiles]]
            leiningen.core.main
            leiningen.show-profiles
            [robert.hooke :refer [add-hook]]))

(defn setup-ns-hook
  "A hook to set up the namespace with the configuration before running a task.
   Only meant to hook leiningen.core.main/apply-task."
  [task & [task-name project & args]]
  (let [configured-project (merge-profiles project
                                           (get-current-profiles))]
    (output-config-namespace configured-project)
    (if (get-in configured-project [:slothcfg :verbose])
      (println "Performing task" task-name "with profiles"
               (:included-profiles (meta configured-project))))
    (apply task task-name configured-project args)))

(defn profiles-task-hook
  "This is a hook for the profiles task, so it will print the current sticky
   profiles after it does whatever it does."
  [task & args]
  (apply task args)
  ;; Only print current profiles when no args on command line (first arg is
  ;; project).
  (when (== 1 (count args))
    (println "")
    (print-current-sticky-profiles (get-current-profiles))))

(defn hooks
  "automatically called by leiningen when slothcfg part of :plugins"
  []
  (add-hook #'leiningen.core.main/apply-task setup-ns-hook)
  (add-hook #'leiningen.show-profiles/show-profiles profiles-task-hook))

