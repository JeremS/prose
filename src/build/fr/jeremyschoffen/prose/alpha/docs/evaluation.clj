(ns fr.jeremyschoffen.prose.alpha.docs.evaluation
  (:require
    [clojure.java.io :as io]

    [fr.jeremyschoffen.prose.alpha.document.sci :as doc]
    [fr.jeremyschoffen.prose.alpha.document.sci.bindings :as bindings]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]


    fr.jeremyschoffen.prose.alpha.docs.tags
    fr.jeremyschoffen.prose.alpha.out.html.tags))


(defn wrap-exception [f phase]
  (fn [& args]
    (try
      (apply f args)
      (catch Exception e
        (throw (ex-info (str "Error making the document during : " phase)
                        {:phase phase}
                        e))))))


(def docs-root "fr/jeremyschoffen/prose/alpha/docs/pages/")


(defn slurp-doc* [path]
   (-> docs-root
       (str path)
       io/resource
       slurp))


(def slurp-doc (wrap-exception slurp-doc* :slurp))


(def read-doc (wrap-exception reader/read-from-string :read))


(def sci-nss {:namespaces
              (bindings/make-ns-bindings
                fr.jeremyschoffen.prose.alpha.docs.tags
                fr.jeremyschoffen.prose.alpha.out.html.tags)})



(def sci-ctxt (doc/init sci-nss))


(def eval-forms (wrap-exception (partial eval-sci/eval-forms-in-temp-ns sci-ctxt) :eval))


(def eval-doc (doc/make-evaluator {:slurp-doc slurp-doc
                                   :read-doc reader/read-from-string
                                   :eval-forms eval-forms}))


(defn document
  ([path]
   (document path {}))
  ([path input]
   (-> path
       (eval-doc (merge input {::slurp-doc slurp-doc}))
       cplr/compile!)))



(comment
  (def doc
    (document "README.md.prose"
              {:project/coords
               {}}))


  (spit "README-test.MD" doc)

  (-> *e
      ex-cause)
      ;ex-data)

  (slurp-doc "readme/example-tags.clj"))
