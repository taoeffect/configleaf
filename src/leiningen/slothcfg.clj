(ns leiningen.slothcfg
  (:refer-clojure :exclude [set])
  (:require [clojure.set :as s]
            [clojure.java.io :as io]
            [slothcfg.core :refer [get-current-profiles unstickable-profiles
                                   fully-expand-profile save-current-profiles
                                   print-current-sticky-profiles]]))

(defn- set
  "Enable the specified profile(s)."
  [project args]
  (let [arg-profiles (into #{} (map keyword args))
        current-profiles (into #{} (get-current-profiles))
        project-profiles (into #{} (keys (:profiles project)))
        unstickable-profiles (unstickable-profiles project)
        known-profiles (s/union project-profiles #{:default :dev :test :user})
        new-profiles (s/union current-profiles arg-profiles)
        basic-new-profiles (reduce s/union #{}
                                   (map (partial fully-expand-profile
                                                 project)
                                        new-profiles))]
    ;; Check if the new profiles are trying to set some combination not allowed
    ;; by the unstickable profiles.
    (if (some #(s/subset? % basic-new-profiles) unstickable-profiles)
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

(defn- unset
  "Disable the specified profile(s). --all to unset all."
  [project args]
  (let [flags (into #{} (map #(.toLowerCase (.substring % 2))
                             (filter #(.startsWith % "--") args)))
        args (filter #(not (.startsWith % "--")) args)
        current-profiles (into #{} (get-current-profiles))
        arg-profiles (into #{} (map keyword args))
        new-profiles (s/difference current-profiles arg-profiles)]
    (if (contains? flags "all")
      (do (save-current-profiles "." #{})
          (println "All profiles unset."))
      ;; No --all flag, so just unset the profiles mentioned.
      (do (save-current-profiles "." new-profiles)
          (print-current-sticky-profiles new-profiles)))))

(defn slothcfg
  "Set or unset sticky profiles."
  {:subtasks [#'set #'unset]}
  [project subtask & args]
  (case subtask
    "set" (set project args)
    "unset" (unset project args)))
