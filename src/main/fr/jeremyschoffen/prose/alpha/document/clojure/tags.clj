(ns fr.jeremyschoffen.prose.alpha.document.clojure.tags
  (:require
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval]))


(defn- get-load-doc []
  (:prose.alpha.document/load-doc eval/*evaluation-env*))


(defn- load* [path form error-msg]
  (let [load-doc (get-load-doc)]
    (try
      (load-doc path)
      (catch Exception e
        (throw (ex-info error-msg
                        {:path path
                         :form form}
                        e))))))


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

  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))

  (binding [eval/*evaluation-env* (assoc eval/*evaluation-env*
                                    :prose.alpha.document/load-doc load-doc)]
    (-> "clojure/master.tp"
        load-doc
        eval/eval-forms-in-temp-ns)))
