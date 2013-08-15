(ns configsloth.test.core
  (:require [configsloth.core :refer [expand-profile
                                      fully-expand-profile
                                      unstickable-profiles]]
            [clojure.test :refer [deftest is]]))

(deftest expand-profile-test
  (is (= [:a] (expand-profile {:profiles {:a {} :b {}}} :a)))
  (is (= [:a] (expand-profile {:profiles {:a {} :b [:a]}} :b)))
  (is (= [:b] (expand-profile {:profiles {:a {} :b [:a] :c [:b]}} :c))))

(deftest fully-expand-profile-test
  (is (= #{:a} (fully-expand-profile {:profiles {:a {} :b {}}} :a)))
  (is (= #{:a} (fully-expand-profile {:profiles {:a {} :b [:a]}} :b)))
  (is (= #{:a} (fully-expand-profile {:profiles {:a {} :b [:a] :c [:b]}} :c)))
  (is (= #{:a :b} (fully-expand-profile {:profiles {:a {} :b {} :c [:a] :d [:b]
                                                    :e [:c :d]}} :e)))
  (is (= #{:a :b} (fully-expand-profile {:profiles {:a {} :b {} :c [:a]
                                                    :d [:c :b]}}
                                        :d))))

(deftest unstickable-profiles-test
  (is (= #{#{:a :b}}
         (unstickable-profiles {:profiles {:a {} :b {} :c {} :d [:a :b]}
                                :configsloth {:never-sticky [:d]}})))
  (is (= #{#{:a :c} #{:b :d}}
         (unstickable-profiles {:profiles {:a {} :b {} :c {} :d {}
                                           :ac [:a :c] :bd [:b :d]}
                                :configsloth {:never-sticky [:ac :bd]}}))))

