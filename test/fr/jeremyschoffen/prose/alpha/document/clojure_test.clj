(ns fr.jeremyschoffen.prose.alpha.document.clojure-test
  (:require
    [clojure.test :refer :all]
    [clojure.java.io :as io]
    [meander.epsilon :as m]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]))


(defn load-doc [path]
  (-> path
      io/resource
      slurp
      reader/read-from-string))



(def doc (binding [eval/*evaluation-env* (assoc eval/*evaluation-env*
                                           :prose.alpha.document/load-doc load-doc
                                           :prose.alpha.document/eval-doc eval/eval-forms-in-temp-ns)]
           (-> "clojure/master.tp"
               load-doc
               eval/eval-forms-in-temp-ns)))


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
