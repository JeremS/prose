(ns fr.jeremyschoffen.prose.alpha.document.clojure
  (:require
    [clojure.java.io :as io]
    [fr.jeremyschoffen.prose.alpha.document.common.evaluator :as evaluator]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))


(defn default-slurp-doc [path]
  (-> path
      io/resource
      slurp))

(def default-env {:slurp-doc default-slurp-doc
                  :read-doc reader/read-from-string
                  :eval-forms eval-common/eval-forms-in-temp-ns})


(defn make-evaluator
  ([]
   (make-evaluator {}))
  ([env]
   (evaluator/make (merge default-env env))))


(comment
  (def eval-doc (make-evaluator))

  (eval-doc "complex-doc/master.prose"))
