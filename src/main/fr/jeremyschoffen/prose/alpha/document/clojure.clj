(ns ^{:author "Jeremy Schoffen"
      :doc "
API providing evaluation tools to evaluate document using Clojure's environment.
"}
  fr.jeremyschoffen.prose.alpha.document.clojure
  (:require
    [clojure.java.io :as io]
    [fr.jeremyschoffen.prose.alpha.document.common.evaluator :as evaluator]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))


(defn default-slurp-doc
  "Similar to `clojure.core/slurp` except that the provided `path` will be treated as a java resource."
  [path]
  (-> path
      io/resource
      slurp))


(def default-env
  "A map of the default functions to use with [[fr.jeremyschoffen.prose.alpha.document.common.evaluator/make]].

  Namely:
  - `:slurp-doc`: [[default-slurp-doc]]
  - `:read-doc`: [[fr.jeremyschoffen.prose.alpha.reader.core/read-from-string]]
  - `:eval-forms`: [[fr.jeremyschoffen.prose.alpha.eval.common/eval-forms-in-temp-ns]]
  "
  {:slurp-doc default-slurp-doc
   :read-doc reader/read-from-string
   :eval-forms eval-common/eval-forms-in-temp-ns})


(defn make-evaluator
  "Simillar to [[fr.jeremyschoffen.prose.alpha.document.common.evaluator/make]] with
  [[default-env]] used as a default."
  ([]
   (make-evaluator {}))
  ([env]
   (evaluator/make (merge default-env env))))


(comment
  (def eval-doc (make-evaluator))

  (eval-doc "complex-doc/master.prose"))
