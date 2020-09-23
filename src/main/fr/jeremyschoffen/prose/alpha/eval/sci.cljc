(ns fr.jeremyschoffen.prose.alpha.eval.sci
  (:refer-clojure :exclude [eval])
  (:require
    [medley.core :as medley]
    [sci.core :as sci]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


;;----------------------------------------------------------------------------------------------------------------------
;; Eval inside sci
;;----------------------------------------------------------------------------------------------------------------------
(def eval-ns
  '(do
     (ns fr.jeremyschoffen.prose.alpha.eval.sci)


     (defn wrap-eval-exception
       "Wraps an eval function so that exceptions thrown are caught and rethrown in an ex-info
       containing the form that threw in its ex-data."
       [e]
       (fn [form]
         (try
           (e form)
           (catch #?(:clj Exception :cljs js/Error) e
             (throw (ex-info "Error during evaluation."
                             {:form form}
                             e))))))


     (defn eval-form->eval-forms
       "Turn an function that evals a form into a function the evals a sequence of forms in sequence.
       The returned function returns the sequence of evaluation results."
       [eval-form]
       (fn [forms]
         (into []
               (map eval-form)
               forms)))


     (defn make-eval-ctxt
       "Make a basic evaluation context."
       [eval-form forms]
       (let [eval-form (wrap-eval-exception eval-form)]
         {:eval/forms forms
          :eval/eval-form eval-form
          :eval/eval-forms (eval-form->eval-forms eval-form)}))


     (defn eval-ctxt
       "Performs the evaluation described by the `ctxt` map.

       Return the context with the evaluation result associated under the key `:eval/result`.
       If the evaluation throws, associates the exception under the
       key `:eval/error` instead."
       [{:eval/keys [forms eval-forms] :as ctxt}]
       (let [[ret res] (try
                         [:eval/result (eval-forms forms)]
                         (catch #?(:clj Exception :cljs js/Error) e
                           [:eval/error e]))]
         (assoc ctxt ret res)))


     (defn wrap-eval-result
       "Middleware that either return the result of the evaluation or throw any error raised."
       [eval-ctxt*]
       (fn [ctxt]
         (let [{:eval/keys [result error]} (eval-ctxt* ctxt)]
           (if result
             result
             (throw error)))))


     (defn wrap-snapshot-ns [eval-ctxt*]
       "Middleware making sure the current ns stays the same after an evaluation."
       (fn [{:eval/keys [eval-form] :as ctxt}]
         (let [current-ns (eval-form '(-> *ns* str symbol))
               ret (eval-ctxt* ctxt)]
           (eval-form (list 'in-ns (list 'quote current-ns)))
           ret)))


     (defn wrap-eval-in-temp-ns
       "Middleware that makes the evaluation take place in a temporary namespace."
       ([eval-ctxt*]
        (wrap-eval-in-temp-ns eval-ctxt* (gensym "temp_ns__")))
       ([eval-ctxt* temp-ns]
        (fn [{:eval/keys [eval-form] :as ctxt}]
          (let [res (do
                      (eval-form (list 'ns temp-ns))
                      (eval-ctxt* ctxt))]
            (eval-form (list 'remove-ns (list 'quote temp-ns)))
            res))))


     (def wrap-eval-forms-in-temp-ns
       "Middleware used in [[eval-forms-in-temp-ns]] namely:
       - [[wrap-eval-result]]
       - [[wrap-snapshot-ns]]
       - [[wrap-eval-in-temp-ns]]"
       (comp wrap-eval-result
             wrap-snapshot-ns
             wrap-eval-in-temp-ns))


     (defn eval-forms-in-temp-ns
       "Evaluate a sequence of forms in a temporary namespace.

       Args:
       - `forms`; a sequence of forms to eval
       - `eval-form`: a function a evaluates one form defaulting to sci own eval."
       ([forms]
        (eval-forms-in-temp-ns eval forms))
       ([eval-form forms]
        (let [ctxt (make-eval-ctxt eval-form forms)
              eval-forms* (wrap-eval-forms-in-temp-ns eval-ctxt)]
          (eval-forms* ctxt))))


     (def wrap-eval-forms
       "Middleware used in [[eval-forms]] namely:
       - [[wrap-eval-result]]
       - [[wrap-snapshot-ns]]"
       (comp wrap-eval-result
             wrap-snapshot-ns))


     (defn eval-forms
       "Evaluate a sequence of forms ensuring the the current namespace doesn't change after the evaluation."
       ([forms]
        (eval-forms eval forms))
       ([eval-form forms]
        (let [ctxt (make-eval-ctxt eval-form forms)
              eval-forms* (wrap-eval-forms eval-ctxt)]
          (eval-forms* ctxt))))))


;;----------------------------------------------------------------------------------------------------------------------
;; Utilities
;;----------------------------------------------------------------------------------------------------------------------
(defn install-code [sci-ctxt code]
  (sci/binding [sci/ns @sci/ns]
    (sci/eval-form sci-ctxt code))
  sci-ctxt)


(def features #?(:clj #{:clj}
                 :cljs #{:cljs}
                 :default #{}))


(def features-opt {:features features})


(def println-binding #?(:clj {}
                        :cljs {'clojure.core {'println println}}))

(def println-opt {:namespaces println-binding})

(defn init
  "Create a sci evaluation context.

  Same as [[sci.core/init]] with the [[eval-ns]] installed. The goal is to provide code executed by sci
  an environment that has a namespace equivalent to [[fr.jeremyschoffen.prose.alpha.eval.clojure]] pre-installed
  in the namespace `fr.jeremyschoffen.prose.alpha.eval.sci`."
  [opts]
  (let [sci-ctxt (->> opts
                      (medley/deep-merge features-opt)
                      sci/init)]
    (install-code sci-ctxt eval-ns)
    sci-ctxt))

(comment
  (def env (init println-opt))

  (sci/binding [sci/ns @sci/ns
                sci/out *out*]
    (sci/eval-form env
                   '(do
                      (in-ns 'fr.jeremyschoffen.prose.alpha.eval.sci)
                      (eval-forms-in-temp-ns '[(+ 1 2 3)
                                               (println (str *ns*))
                                               (throw (ex-info "some msg" {:toto 1}))]))))

  ;; clj
  (-> *e ex-data) ;; should contains faulty form
  (-> *e ex-message)
  (-> *e ex-cause ex-cause ex-data) ;; clj
  (-> *e ex-cause ex-data) ;; cljs
  (-> *e ex-cause  ex-message)


  (sci/binding [sci/ns @sci/ns
                sci/out *out*]
    (doseq [f '[(println *ns*)
                (ns foobar)
                (def inc* inc)
                (inc* 3)
                (println *ns*)]]
      (sci/eval-form env f)))


  (sci/binding [sci/ns @sci/ns
                sci/out *out*]
    (sci/eval-form env
      '(contains? (into (sorted-set)
                        (comp
                          (map str)
                          (map keyword))
                        (all-ns))
                  :foobar))))


;;----------------------------------------------------------------------------------------------------------------------
;; Utilities
;;----------------------------------------------------------------------------------------------------------------------
(defn sci-ctxt->sci-eval
  "Make an eval function from an sci context.

  The result is a function of one argument, a form to be evaluated by sci in the evaluation context `ctxt`."
  [ctxt]
  (fn [form]
    (sci/eval-form ctxt form)))


(defn wrap-sci-bindings
  "Middle for evaluation functions that return an performing the evaluation with the sci `bindings` properly set."
  [eval-fn bindings]
  (fn [form]
    (sci/with-bindings bindings
      (eval-fn form))))


(defn make-sci-eval
  "Make an eval function from a sci context.

  The result function is a function of one argument, a form to be evaluated. It automatically binds
  [[sci.core/ns]] to its dereferenced value."
  ([]
   (make-sci-eval (init nil)))
  ([sci-ctxt]
   (-> sci-ctxt
       sci-ctxt->sci-eval
       (wrap-sci-bindings {sci/ns @sci/ns}))))


(defn eval-forms-in-temp-ns
  "Evaluate a sequence of forms with sci in a temporary namespace."
  ([forms]
   (eval-forms-in-temp-ns (init nil) forms))
  ([sci-ctxt forms]
   (let [ef (make-sci-eval sci-ctxt)]
     (eval-common/eval-forms-in-temp-ns ef forms))))

(comment
  (sci/binding [sci/out *out*]
    (eval-forms-in-temp-ns '[(+ 1 2 3)
                             (println *ns*)
                             (throw (ex-info "some msg" {:toto 1}))]))

  (-> *e ex-data) ;; should contains faulty form
  (-> *e ex-cause ex-cause ex-message)
  (-> *e ex-cause ex-cause ex-data))



(defn eval-forms
  "Evaluate a sequence of forms with sci ensuring the the current namespace doesn't change after the evaluation."
  ([forms]
   (eval-forms (init nil) forms))
  ([sci-ctxt forms]
   (let [ef (make-sci-eval sci-ctxt)]
     (eval-common/eval-forms ef forms))))


(comment
  (sci/binding [sci/out *out*]
    (eval-forms '[(println *ns*)
                  (ns foobar)
                  (def inc* inc)
                  (inc* 3)
                  (println *ns*)]))

  (sci/binding [sci/out *out*]
    (eval-forms '[(println *ns*)
                  (ns foobar)
                  (def inc* inc)
                  (inc* 3)
                  (println *ns*)]))


  (contains? (into (sorted-set)
                   (comp
                     (map str)
                     (map keyword))
                   (all-ns))
             :foobar))