(ns fr.jeremyschoffen.prose.alpha.document.clojure.core
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]))


(defn get-env [] eval/*evaluation-env*)


(defn get-document-inputs []
  (:prose.alpha/document-input (get-env)))


(defn get-load-fn []
  (:prose.alpha/load-fn (get-env)))