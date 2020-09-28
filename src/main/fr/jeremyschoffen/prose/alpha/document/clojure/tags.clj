(ns fr.jeremyschoffen.prose.alpha.document.clojure.tags
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


(defn- get-load-doc []
  (eval-common/get-env :prose.alpha.document/load-doc))


(defn- get-eval-doc []
  (eval-common/get-env :prose.alpha.document/eval-doc))



(defn- load* [load-doc {path :path
                        error-msg :error-msg
                        :as ctxt}]
  (try
    (eval-common/bind-env {:prose.alpha.document/path path}
      (load-doc path))
    (catch Exception e
      (throw (ex-info error-msg
                      (dissoc ctxt :error-msg)
                      e)))))


(defmacro insert-doc [path]
  {:tag :prose.alpha/fragment
   :attrs {}
   :content (load* (get-load-doc)
                   {:path path
                    :form &form
                    :error-msg "Error inserting doc."})})


(defmacro require-doc [path]
  {:tag :prose.alpha/fragment
   :attrs {}
   :content (load* (comp (get-eval-doc)
                         (get-load-doc))
                   {:path path
                    :form &form
                    :error-msg "Error inserting doc."})})



(comment
  (into (sorted-set)
        (comp
          (map str)
          (map keyword))
        (all-ns))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])

  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))

  (eval-common/bind-env {:prose.alpha.document/load-doc load-doc
                         :prose.alpha.document/eval-doc eval-common/eval-forms-in-temp-ns}
    (-> "clojure/master.tp"
        load-doc
        eval-common/eval-forms-in-temp-ns)))
