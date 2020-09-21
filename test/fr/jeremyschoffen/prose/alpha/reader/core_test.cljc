(ns fr.jeremyschoffen.prose.alpha.reader.core-test
  (:require
    #?(:clj [clojure.test :as test :refer [deftest testing is are]]
       :cljs [cljs.test :as test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.reader.core :as c]))



(def simple-form '(+ 1 2 3))
(def simple-form-textp (str \◊ simple-form \◊))


(deftest round-trips
  (is (= simple-form (first (c/read-from-string simple-form-textp)))))


(def example1
  "some text and ◊a-tag")

(deftest form->text
  (is (= "◊a-tag"
         (-> example1
             (c/read-from-string {:keep-comments true})
             second
             (c/form->text example1)))))



