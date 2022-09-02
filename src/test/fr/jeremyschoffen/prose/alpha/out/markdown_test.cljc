(ns fr.jeremyschoffen.prose.alpha.out.markdown-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as md-tags]
    [fr.jeremyschoffen.prose.alpha.out.html.tags :as tags]))


(def html-example [(tags/html5-dtd)
                   (tags/html
                     (tags/head)
                     (tags/body
                       {:class "a b c"}
                       "Some text"
                       (tags/ul
                         (for [i (range 3)]
                           (tags/li i)))))])


(deftest ex-test
  (is (= (cplr/compile! html-example)
         "<!DOCTYPE html>\n<html><head></head><body class=\"a b c\">Some text<ul><li>0</li><li>1</li><li>2</li></ul></body></html>")))




(deftest link
  (is (= (cplr/compile! [(tags/a {:href "http://some.url.com"} "some link")])
         "[some link](http://some.url.com)"))

  (is (= (cplr/compile! [(tags/a {:href "http://some.url.com"})])
         "[http://some.url.com](http://some.url.com)")))



(deftest code-blocks
  (is (= (cplr/compile! (md-tags/code-block "(-> 1 (inc))"))
         "```text\n(-> 1 (inc))\n```")))


(deftest escaping-rules
  (testing "characters are not escapded in normal md"
    (is (= (cplr/compile! ">") ">")))

  (testing "characters are escaped inside regular html"
    (is (= (cplr/compile!
             ["<>" (tags/p "<>")])
           "<><p>&lt;&gt;</p>")))

  (testing "charaters aren't escaped in markdown tags"))



(deftest comments
  (is (= (cplr/compile! (tags/comment "some comment")) "")))
