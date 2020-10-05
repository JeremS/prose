(ns fr.jeremyschoffen.prose.alpha.document.sci-test
  (:require
    #?@(:clj [[clojure.test :refer [deftest is are]]
              [clojure.java.io :as io]]
        :cljs [[cljs.test :refer-macros [deftest is are]]])

    [meander.epsilon :as m :include-macros true]
    [sci.core :as sci :include-macros true]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.document.sci :as document]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci :include-macros true]))


#?(:clj
   (do
    (def resource io/resource)
    (def read-file slurp))

   :cljs
   (do
     (def resource {"complex-doc/master.prose" "test-resources/complex-doc/master.prose"
                    "complex-doc/section-1.prose" "test-resources/complex-doc/section-1.prose"
                    "complex-doc/section-2.prose" "test-resources/complex-doc/section-2.prose"})

     (def fs (js/require "fs"))

     (defn read-file [p]
       (str (.readFileSync fs p)))))


(defn load-doc [path]
  (-> path
      resource
      read-file
      reader/read-from-string))


(def ctxt (document/init {}))

(def eval-doc (document/make-evaluator
                {:load-doc load-doc
                 :eval-forms (partial eval-sci/eval-forms-in-temp-ns ctxt)}))


(def doc (eval-doc "complex-doc/master.prose"))


(def ns-tags (filterv #(and (map? %)
                            (= :ns (:tag %)))
                      (tree-seq map?
                                :content
                                {:tag :doc :content doc})))


(deftest insert-require-docs
  (let [[first-tag second-tag third-tag fourth-tag] ns-tags]
    (are [x y] (= x y)
      first-tag second-tag
      first-tag fourth-tag)
    (is (not= first-tag third-tag))))
