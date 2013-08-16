(defproject slothcfg "1.0.0"
  :description "Persistent and buildable profiles in Leiningen."
  :dependencies [[stencil "0.3.2"]]
  :eval-in-leiningen true
  :profiles {:test {:params {:test 1}
                    :java-properties {"test" "1"
                                      :test2 "2"}}
             :a {}
             :b {}
             :c {}
             :d [:a :b]}
  :slothcfg {:never-sticky [:d] :verbose true}
  ; leave this at the bottom of the file because Sublime's Clojure
  ; syntax highlighting doesn't properly handle quoted quotes within
  ; a regex expression. lein-set-version is specified in profiles.clj
  :set-version {:updates
                [{:path "README.md"
                  :search-regex #"slothcfg \"\d+\.\d+\.\w+?\""}]})