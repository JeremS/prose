(ns fr.jeremyschoffen.prose.alpha.document.sci.env
  (:require
    [medley.core :as medley]
    [sci.core :as sci]

    [fr.jeremyschoffen.prose.alpha.document.sci.bindings :as bindings :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci]
    [fr.jeremyschoffen.prose.alpha.lib.core]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))




;;----------------------------------------------------------------------------------------------------------------------
;; Environment vars
;;----------------------------------------------------------------------------------------------------------------------
(def sci-opt-env-ns {:namespaces (bindings/make-ns-bindings fr.jeremyschoffen.prose.alpha.lib.core)})


(def sci-opt-println eval-sci/sci-opt-println)


(def tags-ns
  '(do
     (ns fr.jeremyschoffen.prose.alpha.document.sci.tags
       (:require
         [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-common]))


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
         (catch #?@(:clj [Exception e]
                    :cljs [js/Error e])
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
                         :error-msg "Error inserting doc."})})))


(defn init [opts]
  (let [opts (medley/deep-merge sci-opt-env-ns opts)
        sci-ctxt (eval-sci/init opts)]
    (eval-sci/install-code sci-ctxt tags-ns)))

(comment
  (def ctxt (init {}))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])



  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))


  (eval-common/bind-env {:prose.alpha.document/load-doc load-doc
                         :prose.alpha.document/eval-doc (partial eval-sci/eval-forms-in-temp-ns ctxt)}

    (->> "sci/master.tp"
         load-doc
         (eval-sci/eval-forms-in-temp-ns ctxt))))

