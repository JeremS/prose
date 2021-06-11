(ns fr.jeremyschoffen.prose.alpha.docs.evaluation
  (:require
    [clojure.java.io :as io]

    [fr.jeremyschoffen.prose.alpha.document.clojure :as doc]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]))



(defn wrap-exception [f phase]
  (fn [& args]
    (try
      (apply f args)
      (catch Exception e
        (throw (ex-info (str "Error making the document during : " phase)
                        {:phase phase
                         :args args}
                        e))))))


(def docs-root "fr/jeremyschoffen/prose/alpha/docs/pages/")


(defn slurp-doc* [path]
   (-> docs-root
       (str path)
       io/resource
       slurp))


(def slurp-doc (wrap-exception slurp-doc* :slurp))


(def read-doc (wrap-exception reader/read-from-string :read))


(def eval-forms (wrap-exception (partial eval-common/eval-forms-in-temp-ns) :eval))


(def eval-doc (doc/make-evaluator {:slurp-doc slurp-doc
                                   :read-doc read-doc
                                   :eval-forms eval-forms}))


(defn document
  ([path]
   (document path {}))
  ([path input]
   (-> path
       (eval-doc input)
       cplr/compile!)))



(comment
  (def doc
    (document "README.md.prose"
              {:project/coords
               {}}))


  (spit "README-test.MD" doc)

  (slurp-doc "readme/example-tags.clj"))
