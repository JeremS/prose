(ns fr.jeremyschoffen.prose.alpha.reader.grammar-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.reader.grammar :as g]
    [instaparse.core :as insta]
    ;[instaparse.combinators]
    [meander.epsilon :as m :include-macros true]
    [lambdaisland.regal :as regal]))


;;----------------------------------------------------------------------------------------------------------------------
;; Lexer
;;----------------------------------------------------------------------------------------------------------------------
(deftest plain-text-r
  (is (= (re-matches g/plain-text "Some text @# [({")
         "Some text @# [({"))

  (is (nil? (re-matches g/plain-text "Some text ◊"))))


(deftest text-symbol-r
  (testing "We can match simple symbols."
    (are [x y] (= (re-matches g/symbol-text x) y)
               "simple-sym" ["simple-sym" nil "simple-sym"]
               "1wrong-sym" nil))

  (testing "We can match namespace qualified symbols."
    (are [x y] (= (re-matches g/symbol-text x) y)
               "some.ns/simple-sym" ["some.ns/simple-sym" "some.ns" "simple-sym"]
               "v3ry.3e#rd/sym23" ["v3ry.3e#rd/sym23" "v3ry.3e#rd" "sym23"]
               "#Wrong.ns/good-sym" nil)))


(deftest verbatim-text-r
  (is (= (re-matches g/verbatim-text "some code inside parens, allowed: () [] {}")
         "some code inside parens, allowed: () [] {}"))

  (is (nil? (re-matches g/verbatim-text "some dangling \""))))


(deftest clojure-strting-r
  (is (= (re-matches g/clojure-string "\"some code inside parens, allowed: () [] {}\\\" \"")
         "\"some code inside parens, allowed: () [] {}\\\" \""))

  (is (nil? (re-matches g/verbatim-text "\"some dangling \"\""))))


(deftest clojure-call-text-r
  (is (= (re-matches g/clojure-call-text "some code inside parens, allowed: [] {}")
         "some code inside parens, allowed: [] {}"))

  (is (nil? (re-matches g/clojure-call-text "f1 (f2")))
  (is (nil? (re-matches g/clojure-call-text "f )")))
  (is (nil? (re-matches g/clojure-call-text "str \"1\"")))
  (is (nil? (re-matches g/clojure-call-text "str ◊\"◊\""))))


(deftest tag-clj-arg-text-r
  (is (= (re-matches g/tag-clj-arg-text "some code inside brackets, allowed; () {}")
         "some code inside brackets, allowed; () {}"))

  (is (nil? (re-matches g/tag-clj-arg-text "f1 [f2")))
  (is (nil? (re-matches g/tag-clj-arg-text "f ]")))
  (is (nil? (re-matches g/tag-clj-arg-text "str \"1\"")))
  (is (nil? (re-matches g/tag-clj-arg-text "str ◊\"◊\""))))


(deftest tag-text-arg-text-r
  (is (= (re-matches g/tag-text-arg-text "some text inside braces, allowed: () [] \"")
         "some text inside braces, allowed: () [] \""))

  (is (nil? (re-matches g/tag-text-arg-text "f1 {f2")))
  (is (nil? (re-matches g/tag-text-arg-text "f }")))
  (is (nil? (re-matches g/tag-text-arg-text "str ◊\"◊\""))))


;; ---------------------------------------------------------------------------------------------------------------------
;; Grammar
;; ---------------------------------------------------------------------------------------------------------------------
(deftest simple-usages
  (testing "Verbatim"
    (testing "Result in text treated as non special."
      (is (= (g/parser "some text ◊\"◊ \" some text after")
             '{:tag :doc, :content ("some text " "◊ " " some text after")})))

    (testing "We can escaped characters notably double quotes in verbatim text"
      (let [res (->> "begin-v ◊\"◊ some \"verbatim text\" end-v"
                     g/parser
                     :content
                     (apply str))

            res2 (->> "begin-v ◊\"◊ some \\\"verbatim text\" end-v"
                      g/parser
                      :content
                      (apply str))]


        (testing "Here the non escaped quotes close the verbatim early"
          (is (=  res
                  "begin-v ◊ some verbatim text\" end-v")))

        (testing "Here the escaped quotes do not close the verbatim early the quotes stay in the middle of the verbatim text"
          (is (=  res2
                  "begin-v ◊ some \"verbatim text end-v")))

        res)))


  (testing "Symbol use"
    (is (= (g/parser "some text ◊|sym  some other text")
           '{:tag :doc, :content ("some text " {:tag :symbol-use, :content ("sym")} "  some other text")})))


  (testing "Clojure call"
    (is (= (->> "◊(str v1 \"a \\\" b\")" g/parser :content first :content (apply str))
           "(str v1 \"a \\\" b\")")))


  (testing "Tag function"
    (is (= '{:tag :doc,
             :content ("some text "
                        {:tag :tag,
                         :content ({:tag :tag-name, :content ("div")}
                                   {:tag :tag-clj-arg, :content ("[" ":classes " "[" ":c1 :c2" "]" "]")}
                                   {:tag :tag-text-arg, :content ("{" "some text in div " {:tag :tag, :content ({:tag :tag-name, :content ("div")})} "}")})}
                        " other")}
           (g/parser "some text ◊div [:classes [:c1 :c2]] {some text in div ◊div} other")))

    (is (= '{:tag :doc,
             :content ("some text "
                        {:tag :tag-unspliced,
                         :content ({:tag :tag-name, :content ("div")}
                                   {:tag :tag-clj-arg, :content ("[" ":classes " "[" ":c1 :c2" "]" "]")}
                                   {:tag :tag-text-arg, :content ("{" "some text in div " {:tag :tag, :content ({:tag :tag-name, :content ("div")})} "}")})}
                        " other")}
           (g/parser "some text ◊◊div [:classes [:c1 :c2]] {some text in div ◊div} other")))))


(deftest recursive-use
  (is (= '{:tag :doc,
           :content ("Some text "
                      {:tag :tag,
                       :content ({:tag :tag-name, :content ("div")}
                                 {:tag :tag-text-arg,
                                  :content ("{"
                                            " in div "
                                            "◊"
                                            " "
                                            {:tag :tag,
                                             :content ({:tag :tag-name, :content ("div")}
                                                       {:tag :tag-clj-arg,
                                                        :content ("["
                                                                   ":a "
                                                                   {:tag :clojure-call, :content ("(" "str " "\"a\\\"b\"" ")")}
                                                                   "]")})}
                                            " "
                                            "}")})})}
        (g/parser "Some text ◊div { in div ◊\"◊\" ◊div [:a ◊(str \"a\\\"b\")] }"))))



(deftest error-cases
  (testing "Dangling ◊ are not allowed"
    (is (insta/failure? (g/parser "some text ◊ some other text"))))


  (testing "Dangling quotes"
    (is (insta/failure? (g/parser "some text ◊(str \"aaa\"\")")))

    #_(is (insta/failure? (g/parser "◊div[:class \"c1 c2\" \"]")))))
