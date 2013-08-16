(defproject slothtest-proj "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[slothcfg "1.0.0"]]
  :dependencies [[org.clojure/clojure "1.5.1"]]
  
  :slothcfg {:config-source-path "src"
             :namespace config
             :middleware (fn [p]
                           (assoc p :tester "HELLO!"))}
  
  :profiles {:a {:just "a test"}
             :b {:better than.all.the.rest}
             :verbose
             {:slothcfg {:verbose true}}}
  )
