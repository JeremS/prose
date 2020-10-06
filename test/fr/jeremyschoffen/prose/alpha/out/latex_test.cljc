(ns fr.jeremyschoffen.prose.alpha.out.latex-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    [fr.jeremyschoffen.prose.alpha.out.latex.compiler :as cplr]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib :include-macros true]))


(lib/def-xml-tag begin)
(lib/def-xml-tag end)
(lib/def-xml-tag title)
(lib/def-xml-tag section)


(def example-1
  [(title "TTT")
   (begin "document")
   (section "1")
   "text"
   (section "2")
   "text"
   (end "document")])


(deftest basic
  (is (= (->> example-1
              (interpose "\n")
              cplr/compile!)
         "\\title{TTT}\n\\begin{document}\n\\section{1}\ntext\n\\section{2}\ntext\n\\end{document}")))

(comment
  (->> example-1
       (interpose "\n")
       cplr/compile!
       println))