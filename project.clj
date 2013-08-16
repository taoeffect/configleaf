(defproject slothcfg "1.0.1"
  :description "Build configurations for Leiningen."
  :url "https://github.com/taoeffect/slothcfg"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  
  
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
  
  :deploy-branches ["master"]
  
  :pom-addition [:developers
                 ; created slothcfg fork of configleaf
                 [[:name "Greg Slepak"]
                  [:url "https://twitter.com/taoeffect"]]
                 ; author of PR from which slothcfg was forked
                 [[:name "Justin Balthrop"]
                  [:url "https://github.com/ninjudd"]]
                 ; original author of configleaf
                 [[:name "David Santiago"]
                  [:url "https://github.com/davidsantiago"]]]
  
  ; leave this at the bottom of the file because Sublime's Clojure
  ; syntax highlighting doesn't properly handle quoted quotes within
  ; a regex expression. lein-set-version is specified in profiles.clj
  :set-version {:updates
                [{:path "README.md"
                  :search-regex #"slothcfg \"\d+\.\d+\.\w+?\""}]})