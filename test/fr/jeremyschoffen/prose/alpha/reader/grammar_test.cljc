(ns fr.jeremyschoffen.prose.alpha.reader.grammar-test
  (:require
    #?(:clj [clojure.test :as test :refer [deftest testing is are]]
       :cljs [cljs.test :as test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.reader.grammar :as g]
    [instaparse.core :as insta]
    [instaparse.combinators :as instac]
    [meander.epsilon :as m :include-macros true]
    [lambdaisland.regal :as regal]))


(defn make-insta-rule-parser-with-lookahead [n regex ending]
  (let [ending-c (if (string? ending)
                   (instac/string ending)
                   (instac/regexp ending))
        grammar {n        (instac/cat (instac/nt :regex-r) ending-c)
                 :regex-r (instac/regexp regex)}]
    (insta/parser grammar :start n)))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text free
;; ---------------------------------------------------------------------------------------------------------------------
(deftest plain-text-test
  (testing "Recognizes any text except for the chararcters '◊' and '\\'"
    (are [x y] (= (re-matches g/plain-text x) y)
               "abcd" "abcd"
               "abcd◊" nil
               "abcd\\e" nil)))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text verbatim
;; ---------------------------------------------------------------------------------------------------------------------
(def verbatim-p (make-insta-rule-parser-with-lookahead :p g/text-verbatim "!◊"))

(deftest text-verbatim-test
  (testing "Accepts anny text until finished with \"!◊\"."
    (is (= (verbatim-p "Some text contaning ◊.!◊")
           [:p [:regex-r "Some text contaning ◊."] "!◊"]))

    (is (insta/failure? (verbatim-p "Some text contaning ◊.")))))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text comment
;; ---------------------------------------------------------------------------------------------------------------------
(def comment-p (make-insta-rule-parser-with-lookahead :p g/text-comment "/◊"))

(deftest text-comment-test
  (testing "Accepts anny text until finished with \"/◊\"."
    (is (= (comment-p "Some text contaning ◊./◊")
           [:p [:regex-r "Some text contaning ◊."] "/◊"]))

    (is (insta/failure? (comment-p "Some text contaning ◊.")))))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text symbol
;; ---------------------------------------------------------------------------------------------------------------------
(deftest text-symbol-test
  (testing "We can match simple symbols."
    (are [x y] (= (re-matches g/text-symbol x) y)
               "simple-sym" ["simple-sym" nil "simple-sym"]
               "1wrong-sym" nil))

  (testing "We can match namespace qualified symbols."
    (are [x y] (= (re-matches g/text-symbol x) y)
               "some.ns/simple-sym" ["some.ns/simple-sym" "some.ns" "simple-sym"]
               "v3ry.3e#rd/sym23" ["v3ry.3e#rd/sym23" "v3ry.3e#rd" "sym23"]
               "#Wrong.ns/good-sym" nil)))



;; ---------------------------------------------------------------------------------------------------------------------
;; Text embeded value
;; ---------------------------------------------------------------------------------------------------------------------
(def text-e-value-p (make-insta-rule-parser-with-lookahead :p g/text-e-value "|◊"))

(deftest text-e-value-test
  (testing "We can match simple symbols terminated by \"|◊\"."
    (is (= (text-e-value-p "simple-sym|◊")
           [:p [:regex-r "simple-sym"] "|◊"]))

    (is (insta/failure? (text-e-value-p "good-sym-not-terminated")))

    (is (insta/failure? (text-e-value-p "#wrong-sym|◊"))))


  (testing "We can match namespace qualified symbols terminated by \"|◊\"."
    (is (= (text-e-value-p "some.ns/simple-sym|◊")
           [:p [:regex-r "some.ns/simple-sym"] "|◊"]))
    (is (insta/failure? (text-e-value-p "some/ns/good-sym-not-terminated")))

    (is (insta/failure? (text-e-value-p "#wrong/sym|◊")))))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text embedded code
;; ---------------------------------------------------------------------------------------------------------------------
(def text-e-code-p (make-insta-rule-parser-with-lookahead :p
                                                          g/text-e-code
                                                          (regal/regex g/end-embeded-code)))

(deftest text-e-code-test
  (testing "The text inside embedded clojure code is parsed until \"◊\" or \")◊\""
    (is (= (text-e-code-p "(+ 1 2 3)◊")
           [:p [:regex-r "(+ 1 2 3"] ")◊"]))

    (is (= (text-e-code-p "(+ 1 2 3◊")
           [:p [:regex-r "(+ 1 2 3"] "◊"]))))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text clojure arg to tags
;; ---------------------------------------------------------------------------------------------------------------------
(deftest text-t-clj-test
  (testing "Inside clojure args to tags any text is permitted but the chars \" ◊ [ ] except when escaped."
    (are [x y] (= (re-matches g/text-t-clj x) y)
               "abc" "abc"
               "abc \\\" \\◊ \\[ \\]" "abc \\\" \\◊ \\[ \\]"
               "abc ◊" nil)))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text clojure string
;; ---------------------------------------------------------------------------------------------------------------------
(deftest text-t-clj-str-test
  (testing "Inside clojure strings in clj args to tags any text is permitted but the char \" except when escaped."
    (are [x y] (= (re-matches g/text-t-clj-str x) y)
               "abc"       "abc"
               "abc ◊ [ ]" "abc ◊ [ ]"
               "abc \\\""  "abc \\\""
               "abc \""    nil)))


;; ---------------------------------------------------------------------------------------------------------------------
;; Text string arg to tags
;; ---------------------------------------------------------------------------------------------------------------------
(deftest text-t-txt-test
  (testing "Inside clojure strings args to tags any text is permitted but the chars ◊ } and \\"
    (are [x y] (= (re-matches g/tag-plain-text x) y)
               "abcd" "abcd"
               "abcd◊" nil
               "abcd\\e" nil
               "abc}de" nil)))



(def simple-text
  "Some simple text.")

(defn vec->text [v]
  (clojure.string/join "\n" v))

(def simple-text-with-comment
  (vec->text
    ["Some simple text."
     "◊/A comment "
     "on several lines."
     "/◊"
     "Some other text."]))


(def simple-text-with-verbatim
  (vec->text
    ["Some simple text."
     "◊!Some ◊ verbatim ◊stuff[]{}!◊"
     "Some other text."]))


(def simple-text-with-embeded-code
  (vec->text
    ["Some simple text."
     "◊(+ 1 2 3)◊"
     "Some other text."]))

(def simple-text-with-embeded-value
  (vec->text
    ["Some simple text."
     "◊|@an-atom|◊"
     "Some other text."]))

(def simple-tag
  "Some text with an emphased ◊em[:class \"class\"]{word}. End.")


(deftest parsing-simple-blocks
  (testing "We can parse simple text."
    (is (= (g/parser simple-text)
           '{:tag :doc, :content ("Some simple text.")})))

  (testing "We can parse comments."
    (is (= (g/parser simple-text-with-comment)
           '{:tag :doc,
             :content ("Some simple text.\n"
                        {:tag :comment, :content ("A comment \non several lines.\n")}
                        "\nSome other text.")})))

  (testing "We can parse verbatim text."
    (is (= (g/parser simple-text-with-verbatim)
           '{:tag :doc,
             :content ("Some simple text.\n"
                        {:tag :verbatim, :content ("Some ◊ verbatim ◊stuff[]{}")}
                        "\nSome other text.")})))

  (testing "We can parse embedded code."
    (is (= (g/parser simple-text-with-embeded-code)
           '{:tag :doc, :content ("Some simple text.\n"
                                   {:tag :embedded-code, :content ("(" "+ 1 2 3" ")")}
                                   "\nSome other text.")})))

  (testing "We can parse embedded values."
    (is (= (g/parser simple-text-with-embeded-value)
           '{:tag :doc, :content ("Some simple text.\n"
                                   {:tag :embedded-value, :content ("@an-atom")}
                                   "\nSome other text.")})))
  (testing "We can parse tags."
    (is (= (g/parser simple-tag)
           '{:tag :doc,
             :content ("Some text with an emphased "
                        {:tag :tag,
                         :content ({:tag :tag-name, :content ("em")}
                                   {:tag :tag-args-clj, :content ("[" ":class " "\"" "class" "\"" "]")}
                                   {:tag :tag-args-txt, :content ("word")})}
                        ". End.")}))))


(deftest escaping
  (testing "Escaping in tags"
    (let [t1 "◊t{Some text with an escaped \\}.}"
          t2 "◊t{Some text without an escaped }.}"
          t3 "◊t[:some-param \"]\"]{Some text with bracket in a string in args.}"
          t4 "◊t[:some-param \\]]{Some text with escaped bracket in args.}"]
      (are [x y] (= (g/parser x) y)
                 t1 '{:tag :doc,
                      :content ({:tag :tag,
                                 :content ({:tag :tag-name, :content ("t")}
                                           {:tag :tag-args-txt, :content ("Some text with an escaped " "}" ".")})})}
                 t2 '{:tag :doc,
                      :content ({:tag :tag,
                                 :content ({:tag :tag-name, :content ("t")}
                                           {:tag :tag-args-txt, :content ("Some text without an escaped ")})}
                                ".}")}
                 t3 '{:tag :doc,
                      :content ({:tag :tag,
                                 :content ({:tag :tag-name, :content ("t")}
                                           {:tag :tag-args-clj, :content ("[" ":some-param " "\"" "]" "\"" "]")}
                                           {:tag :tag-args-txt, :content ("Some text with bracket in a string in args.")})})}
                 t4 '{:tag :doc,
                      :content ({:tag :tag,
                                 :content ({:tag :tag-name, :content ("t")}
                                           {:tag :tag-args-clj, :content ("[" ":some-param \\]" "]")}
                                           {:tag :tag-args-txt, :content ("Some text with escaped bracket in args.")})})}))))


(defn parse-first [x]
  (-> x
      g/parser
      (-> :content first)))


(def simple-addition "◊(def x (+ 1 2))◊")
(def simple-addition-parse (parse-first simple-addition))

(def simple-embedding "◊|x|◊")
(def simple-embedding-parse (parse-first simple-embedding))



(def twisted-embedding
  (vec->text
    ["◊(defn template [v]"
     "   ◊div {"
     (str "     " simple-addition)
     (str "     sufixing with: " simple-embedding)
     "    }"
     ")◊"]))

(deftest embedding
  (testing "We can embedd ad nauseam."
    (is (= #{simple-addition-parse
             simple-embedding-parse}

           (set
             (m/search
               (parse-first twisted-embedding)
               {:tag :embedded-code
                :content (m/scan {:tag :tag
                                  :content (m/scan {:tag :tag-args-txt
                                                    :content (m/scan (m/pred map? ?c))})})}

               ?c))))))

;; TODO: see if there is a way to enforce some error that are glossed over by the parser back-tracking
(deftest anticipated-failures
  (testing "The parser doesn't allow single diamonds."
    (is (insta/failure? (g/parser "◊ toto"))))

  #_(testing "The parser wants tag args to be closed"
      (g/parser "◊div { some stuff")))

