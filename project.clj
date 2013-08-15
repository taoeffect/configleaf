(defproject configsloth "1.0.0"
  :description "Persistent and buildable profiles in Leiningen."
  :plugins [[lein-set-version "0.3.0"]]
  :dependencies [[stencil "0.3.2"]]
  :eval-in-leiningen true
  :profiles {:test {:params {:test 1}
                    :java-properties {"test" "1"
                                      :test2 "2"}}
             :a {}
             :b {}
             :c {}
             :d [:a :b]}
  :configsloth {:never-sticky [:d]}
  ; leave this at the bottom of the file because Sublime's Clojure
  ; syntax highlighting doesn't properly handle quoted quotes within
  ; a regex expression.
  :set-version {:updates
                [{:path "README.md"
                  :search-regex #"configsloth \"\d+\.\d+\.\d+\""}]})