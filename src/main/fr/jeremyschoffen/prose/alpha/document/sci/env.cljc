(ns fr.jeremyschoffen.prose.alpha.document.sci.env
  (:require
    [meander.epsilon :as m :include-macros true]
    [medley.core :as medley]

    [fr.jeremyschoffen.prose.alpha.document.sci.bindings :as sci-bindings :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci]

    [fr.jeremyschoffen.prose.alpha.document.clojure.tags]
    [fr.jeremyschoffen.prose.alpha.lib.core]))



(def sci-opt-doc-ns {:namespaces (sci-bindings/make-ns-bindings fr.jeremyschoffen.prose.alpha.document.clojure.tags
                                                                fr.jeremyschoffen.prose.alpha.lib.core)})

(defn init [opts]
  (let [opts (medley/deep-merge sci-opt-doc-ns opts)]
    (eval-sci/init opts)))






(comment
  (def ctxt (init {}))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])



  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))

  (eval-common/bind-env {:prose.alpha.document/load-doc load-doc
                         :prose.alpha.document/eval-doc (partial eval-sci/eval-forms-in-temp-ns ctxt)
                         :prose.alpha.document/path "complex-doc/master.tp"}

    (->> "complex-doc/master.tp"
         load-doc
         (eval-sci/eval-forms-in-temp-ns ctxt))))
