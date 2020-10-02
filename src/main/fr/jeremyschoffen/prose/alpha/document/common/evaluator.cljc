(ns fr.jeremyschoffen.prose.alpha.document.common.evaluator
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


(defn eval-doc [{:prose.alpha.document/keys [path load-doc eval-forms] :as env}]
  (eval-common/bind-env env
    (-> path
        load-doc
        eval-forms)))


(defn make [{load-doc :load-doc
             efs      :eval-forms}]
  (let [env {:prose.alpha.document/eval-forms efs
             :prose.alpha.document/load-doc load-doc}]
    (fn
      ([path]
       (eval-doc (assoc env
                   :prose.alpha.document/path path
                   :prose.alpha.document/input {})))
      ([path inputs]
       (eval-doc (assoc env
                   :prose.alpha.document/path path
                   :prose.alpha.document/input inputs))))))