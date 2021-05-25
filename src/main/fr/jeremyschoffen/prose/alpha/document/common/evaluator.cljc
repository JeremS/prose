(ns ^{:author "Jeremy Schoffen"
      :doc "
Generic API providing document evaluation utilities.
"}
  fr.jeremyschoffen.prose.alpha.document.common.evaluator
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


(defn eval-doc
  "Evaluate a document.

  Takes care of binding [[fr.jeremyschoffen.prose.alpha.eval.common/*evaluation-env*]] using its parameter.

  This function takes one argument which is a map. The keys are as follow:
  - `path`: a string, the path to the document
  - `slurp-doc`: a function that takes a path to a file and return the text content of that file
  - `read-doc`: a reader / parser function, text in, code (data) out
  - `eval-forms`: an evaluation function, takes a sequence of forms and returns the sequence of evaluations
  "
  [{:prose.alpha.document/keys [path slurp-doc read-doc eval-forms] :as env}]
  (eval-common/bind-env env
    (-> path
        slurp-doc
        read-doc
        eval-forms)))


(defn make
  "Make an evaluation function that uses [[eval-doc]] under the hood.

  Takes a map as an argument with the following keys:
  - `slurp-doc`: a function that takes a path to a file and return the text content of that file
  - `read-doc`: a reader / parser function, text in, code (data) out
  - `eval-forms`: an evaluation function, takes a sequence of forms and returns the sequence of evaluations

  Returns a function that takes a path and optionally some input as arguments and returns the evaluated document.
  The input if present is added to [[fr.jeremyschoffen.prose.alpha.eval.common/*evaluation-env*]] under the key
  `:prose.alpha.document/input {}`
  For instance:
  ```clojure
  (def my-eval-doc
       (make {:slurp-doc sd
              :read-doc read-from-string
              :eval-forms eval-forms}))

  (my-eval-doc \"path/to/doc\" {:some :input})
  ```
  "
  [{slurp-doc :slurp-doc
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

