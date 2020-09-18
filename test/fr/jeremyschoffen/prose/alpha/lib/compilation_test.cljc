(ns fr.jeremyschoffen.prose.alpha.lib.compilation-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.lib.compilation :as c :include-macros true]))


(def example-1 ["some text\n" nil [1 2 3]])


(deftest text-compilation-env
  (is (= (c/text-environment
           (apply c/emit! example-1))
         (apply str example-1))))
