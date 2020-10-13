(ns fr.jeremyschoffen.prose.alpha.document.common.evaluator
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


(defn eval-doc [{:prose.alpha.document/keys [path slurp-doc read-doc eval-forms] :as env}]
  (eval-common/bind-env env
    (-> path
        slurp-doc
        read-doc
        eval-forms)))


(defn make [{slurp-doc :slurp-doc
             read-doc :read-doc
             efs      :eval-forms}]
  (let [env {:prose.alpha.document/slurp-doc slurp-doc
             :prose.alpha.document/read-doc read-doc
             :prose.alpha.document/eval-forms efs}]
    (fn
      ([path]
       (eval-doc (assoc env
                   :prose.alpha.document/path path
                   :prose.alpha.document/input {})))
      ([path inputs]
       (eval-doc (assoc env
                   :prose.alpha.document/path path
                   :prose.alpha.document/input inputs))))))