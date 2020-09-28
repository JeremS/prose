(ns fr.jeremyschoffen.prose.alpha.document.sci-test
  (:require
    #?@(:clj [[clojure.test :refer [deftest is are]]
              [clojure.java.io :as io]]
        :cljs [[cljs.test :refer-macros [deftest is are]]])

    [meander.epsilon :as m :include-macros true]
    [sci.core :as sci :include-macros true]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.document.sci.env :as env]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci :include-macros true]))


#?(:clj
   (do
    (def resource io/resource)
    (def read-file slurp))

   :cljs
   (do
     (def resource {"complex-doc/master.tp" "test-resources/complex-doc/master.tp"
                    "complex-doc/section-1.tp" "test-resources/complex-doc/section-1.tp"
                    "complex-doc/section-2.tp" "test-resources/complex-doc/section-2.tp"})

     (def fs (js/require "fs"))

     (defn read-file [p]
       (str (.readFileSync fs p)))))


(defn load-doc [path]
  (-> path
      resource
      read-file
      reader/read-from-string))


(def ctxt (env/init {}))


(def doc (eval-common/bind-env {:prose.alpha.document/load-doc load-doc
                                :prose.alpha.document/eval-doc (partial eval-sci/eval-forms-in-temp-ns ctxt)}
           (->> "complex-doc/master.tp"
                load-doc
                (eval-sci/eval-forms-in-temp-ns ctxt))))


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
