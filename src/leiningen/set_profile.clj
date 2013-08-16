(ns leiningen.set-profile
  (:require [clojure.set :as set]
            [slothcfg.core :refer [get-current-profiles unstickable-profiles
                                   fully-expand-profile save-current-profiles
                                   print-current-sticky-profiles]]))

(defn set-profile
  "Set the profile(s) specified to be active."
  [project & args]
  (let [arg-profiles (into #{} (map keyword args))
        current-profiles (into #{} (get-current-profiles))
        project-profiles (into #{} (keys (:profiles project)))
        unstickable-profiles (unstickable-profiles project)
        known-profiles (set/union project-profiles #{:default :dev :test :user})
        new-profiles (set/union current-profiles arg-profiles)
        basic-new-profiles (reduce set/union #{}
                                   (map (partial fully-expand-profile
                                                 project)
                                        new-profiles))]
    ;; Check if the new profiles are trying to set some combination not allowed
    ;; by the unstickable profiles.
    (if (some #(set/subset? % basic-new-profiles) unstickable-profiles)
      (do
        (println "Error: The set of profiles" arg-profiles
                 "could not be set active because it violates the :never-sticky option.")
        (print-current-sticky-profiles current-profiles))
      ;; else: no unstickable subset is being set active.
      (do
        ;; Print a warning for the unknown profiles, but set them anyways.
        (doseq [unknown-profile (filter #(not (contains? known-profiles %))
                                        new-profiles)]
          (println "Warning: Unknown profile" unknown-profile
                   "is being set active."))
        ;; Check if task was called without args, before saving; calling without
        ;; args should not change anything.
        (if (not (empty? arg-profiles))
          (save-current-profiles "." new-profiles))
        (print-current-sticky-profiles new-profiles)))))
