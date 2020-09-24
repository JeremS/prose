(ns fr.jeremyschoffen.prose.alpha.document.sci-test
  (:require
    #?@(:clj [[clojure.test :refer [deftest is]]
              [clojure.java.io :as io]]
        :cljs [[cljs.test :refer-macros [deftest is]]])

    [meander.epsilon :as m :include-macros true]
    [sci.core :as sci :include-macros true]

    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.document.sci.env :as env]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval]))

#?(:clj
   (do
    (def resource io/resource)
    (def read-file slurp))

   :cljs
   (do
     (def resource {"sci/master.tp" "test-resources/sci/master.tp"
                    "sci/section-1.tp" "test-resources/sci/section-1.tp"
                    "sci/section-2.tp" "test-resources/sci/section-2.tp"})

     (def fs (js/require "fs"))

     (defn read-file [p]
       (str (.readFileSync fs p)))))


(defn load-doc [path]
  (-> path
      resource
      read-file
      reader/read-from-string))


(def ctxt (env/init {}))


(def doc (sci/binding [env/load-document-var load-doc]
           (->> "sci/master.tp"
                load-doc
                (eval/eval-forms-in-temp-ns ctxt))))

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

