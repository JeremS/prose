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
(def load-document-var
  (sci/new-dynamic-var '*load-document*
                       (fn [& _]
                         (throw (ex-info "Unbound load document function." {})))))

(def input-var
  (sci/new-dynamic-var '*input* {}))

(def sci-opt-env-ns {:namespaces
                     (merge {'fr.jeremyschoffen.prose.alpha.document.sci.env {'*load-document* load-document-var
                                                                              '*input* input-var}}
                            (bindings/make-ns-bindings fr.jeremyschoffen.prose.alpha.lib.core))})

(def sci-opt-println eval/sci-opt-println)


(def tags-ns
  '(do
     (ns fr.jeremyschoffen.prose.alpha.document.sci.tags
       (:require
         [fr.jeremyschoffen.prose.alpha.document.sci.env :as env]
         [fr.jeremyschoffen.prose.alpha.eval.sci :as eval]))


     (defn- load* [path form error-msg]
       (try
         (env/*load-document* path)
         (catch #?(:clj Exception :cljs js/Error) e
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
                   (load* path &form "Error requiring doc."))})))


(defn init [opts]
  (let [opts (medley/deep-merge sci-opt-env-ns opts)
        sci-ctxt (eval/init opts)]
    (eval/install-code sci-ctxt tags-ns)))

(comment
  (def ctxt (init {}))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])

  (let [load-doc (fn [path]
                   (-> path
                       io/resource
                       slurp
                       reader/read-from-string))]
    (sci/binding [load-document-var (fn [path]
                                      (-> path
                                          io/resource
                                          slurp
                                          reader/read-from-string))]
      (->> "sci/master.tp"
           load-doc
           (eval/eval-forms-in-temp-ns ctxt)))))

