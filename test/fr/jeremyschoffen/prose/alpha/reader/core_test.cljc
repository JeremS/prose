(ns fr.jeremyschoffen.prose.alpha.reader.core-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.reader.core :as c]))



(def simple-form '(+ 1 2 3))
(def simple-form-textp (str \◊ simple-form))


(deftest round-trips
  (is (= simple-form (first (c/read-from-string simple-form-textp)))))


(def example1
  "some text and ◊a-tag")

(deftest form->text
  (is (= "◊a-tag"
         (-> example1
             c/read-from-string
             second
             (c/form->text example1)))))

(def example
  "Some text. ◊div[{:class ◊str{c1 c2}}] { ◊def[x 1] ◊(def y 2) }")


(deftest complex-example
  (is (= (c/read-from-string example)
         '["Some text. " (div {:class (str "c1 c2")} " " (def x 1) " " (def y 2) " ")])))
