(ns fr.jeremyschoffen.prose.alpha.out.html-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    [fr.jeremyschoffen.prose.alpha.out.html.compiler :as cplr]
    [fr.jeremyschoffen.prose.alpha.out.html.tags :as tags]))


(def ex [(tags/html5-dtd)
         (tags/html
           (tags/head)
           (tags/body
             {:class "a b c"}
             "Some text"
             (tags/ul
               (for [i (range 3)]
                 (tags/li i)))))])


(deftest ex-test
  (is (= (cplr/compile! ex)
         "<!DOCTYPE html>\n<html><head></head><body class=\"a b c\">Some text<ul><li>0</li><li>1</li><li>2</li></ul></body></html>")))

