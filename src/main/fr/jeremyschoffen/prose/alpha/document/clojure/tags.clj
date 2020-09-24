(ns fr.jeremyschoffen.prose.alpha.document.clojure.tags
  (:require
    [fr.jeremyschoffen.prose.alpha.document.clojure.env :as env]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]))


(defn- load* [path form error-msg]
  (try
    (env/*load-document* path)
    (catch Exception e
      (throw (ex-info error-msg
                      {:path path
                       :form form}
                      e)))))


(defmacro insert-doc [path]
  {:tag :prose.alpha/fragment
   :attrs {}
   :content (load* path &form "Error inserting doc.")})


(defmacro require-doc [path]
  {:tag :prose.alpha/fragment
   :attrs {}
   :content (eval/eval-forms-in-temp-ns
              (load* path &form "Error requiring doc."))})



(comment
  (into (sorted-set)
        (comp
          (map str)
          (map keyword))
        (all-ns))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])

  (binding [env/*load-document* (fn [path]
                                  (-> path
                                      io/resource
                                      slurp
                                      reader/read-from-string))]
    (-> "clojure/master.tp"
        env/*load-document*
        eval/eval-forms-in-temp-ns)))
