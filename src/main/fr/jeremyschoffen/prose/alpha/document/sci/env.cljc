(ns fr.jeremyschoffen.prose.alpha.document.sci.env
  (:require
    [medley.core :as medley]
    [sci.core :as sci]

    [fr.jeremyschoffen.prose.alpha.document.sci.bindings :as bindings :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval]
    [fr.jeremyschoffen.prose.alpha.lib.core]))




;;----------------------------------------------------------------------------------------------------------------------
;; Environment vars
;;----------------------------------------------------------------------------------------------------------------------
(def sci-opt-env-ns {:namespaces (bindings/make-ns-bindings fr.jeremyschoffen.prose.alpha.lib.core)})


(def sci-opt-println eval/sci-opt-println)


(def tags-ns
  '(do
     (ns fr.jeremyschoffen.prose.alpha.document.sci.tags
       (:require
         [fr.jeremyschoffen.prose.alpha.eval.sci :as eval]))


     (defn- get-load-doc []
       (:prose.alpha.document/load-doc eval/*evaluation-env*))


     (defn- load* [path form error-msg]
       (let [load-doc (get-load-doc)]
         (try
           (load-doc path)
           (catch #?(:clj Exception :cljs js/Error) e
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
                   (load* path &form "Error requiring doc."))})))


(defn init [opts]
  (let [opts (medley/deep-merge sci-opt-env-ns opts)
        sci-ctxt (eval/init opts)]
    (eval/install-code sci-ctxt tags-ns)))

(comment
  (def ctxt (init {}))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])



  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))

  (sci/binding [eval/eval-env-var (assoc @eval/eval-env-var
                                    :prose.alpha.document/load-doc load-doc)]
    (->> "sci/master.tp"
         load-doc
         (eval/eval-forms-in-temp-ns ctxt))))

