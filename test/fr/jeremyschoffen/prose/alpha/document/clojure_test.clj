(ns fr.jeremyschoffen.prose.alpha.document.clojure-test
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [meander.epsilon :as m]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]

    [fr.jeremyschoffen.prose.alpha.document.clojure :as document]))


(def eval-doc (document/make-evaluator))

(def doc (eval-doc "complex-doc/master.tp"))

(def ns-tags (filterv #(and (map? %)
                            (= :ns (:tag %)))
                       (tree-seq map?
                                 :content
                                 {:tag :doc :content doc})))

(deftest insert-require-dos
  (is  (m/match ns-tags
         [?x1 ..2 ?x2 ?x1]
         true

         _ false)))
