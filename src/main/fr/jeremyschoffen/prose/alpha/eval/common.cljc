(ns fr.jeremyschoffen.prose.alpha.eval.common)


(def ^:dynamic *evaluation-env* {:prose.alpha.eval/env :clojure})


(defn wrap-eval-form-exception
  "Wraps an eval function so that exceptions thrown are caught and rethrown in an ex-info
  containing the form that threw in its ex-data."
  [e]
  (fn [form]
    (try
      (e form)
      (catch #?(:clj Exception :cljs js/Error) e
        (throw (ex-info "Error during evaluation."
                        {:prose.alpha.evaluation/env *evaluation-env*
                         :prose.alpha.evaluation/form form}
                        e))))))


(defn eval-forms*
  "Evaluates a sequences of forms `forms` in sequence with `eval-form`"
  [eval-form forms]
  (into [] (map eval-form) forms))


(defn make-evaluation-ctxt [eval-form forms]
  {:forms forms
   :eval-form (wrap-eval-form-exception eval-form)})


(defn evaluate-ctxt [{:keys [forms eval-form]
                      :as ctxt}]
  (let [[ret res] (try
                    [:result (eval-forms* eval-form forms)]
                    (catch #?(:clj Exception :cljs js/Error) e
                      [:error e]))]
    (assoc ctxt ret res)))


(defn evaluate
  "Evaluate a sequence of forms in order. Returns the sequence of evaluations.

  To do so an 'eval-forms' function is create from `ef`. This function is wrapped by `middleware` to then be passed
  `forms` as parameter.

  Args:
  - `ef`: an 'evaluate-form' function that take 1 form and returns the result of evaulating it.
  - `middleware`: an 'eval-forms -> eval-forms' function
  - `forms`: the sequence to forms to evaluate"
  [ef middleware forms]
  (let [ctxt (make-evaluation-ctxt ef forms)
        eval-ctxt (middleware evaluate-ctxt)]
    (eval-ctxt ctxt)))


(comment
  (evaluate eval
            identity
            '[(+ 1 2)
              (println *ns*)])

  (-> (evaluate eval
                identity
                '[(+ 1 2)
                  (throw (ex-info "some weird exception" {:a :b}))])
      :error
      ex-data))


(defn wrap-eval-result
  "Middleware that either returns the result of the evaluation or throw any error raised."
  [eval-ctxt]
  (fn [ctxt]
    (let [{:keys [result error]} (eval-ctxt ctxt)]
      (if result
        result
        (throw error)))))

(comment
  (evaluate eval
            wrap-eval-result
            '[(+ 1 2)
              (println *ns*)])

  (evaluate eval
            wrap-eval-result
            '[(+ 1 2)
              (throw (ex-info "some weird exception" {:a :b}))])

  (-> *e ex-data))



(def ^:private destructure-ex-info (juxt ex-message ex-data ex-cause))


(defn- middleware-evaluation
  [{:keys [middleware-name middleware-action eval-form form]}]
  (try
    (eval-form form)
    (catch #?(:clj Exception :cljs js/Error) e
      (let [[msg data cause] (destructure-ex-info e)]
        (throw (ex-info msg
                        (assoc data
                          ::middleware-name middleware-name
                          ::action middleware-action)
                        cause))))))


(defn- get-current-ns [eval-form]
  (middleware-evaluation {:middleware-name ::wrap-snapshot-ns
                          :middleware-action ::get-current-ns
                          :eval-form eval-form
                          :form '(-> *ns* str symbol)}))



(defn- back-to-base-ns [eval-form ns-name]
  (middleware-evaluation {:middleware-name ::wrap-snapshot-ns
                          :middleware-action ::back-to-base-ns
                          :eval-form eval-form
                          :form (list 'in-ns (list 'quote ns-name))}))


(defn wrap-snapshot-ns
  "Middleware making sure the current ns stays the same after an evaluation."
  [eval-ctxt]
  (fn [{:keys [eval-form] :as ctxt}]
    (let [current-ns (get-current-ns eval-form)
          ret (eval-ctxt ctxt)]
      (back-to-base-ns eval-form current-ns)
      ret)))


(defn- switch-to-temp-ns [eval-form ns-name]
  (middleware-evaluation {:middleware-name ::wrap-eval-in-temp-ns
                          :middleware-action ::switch-to-temp-ns
                          :eval-form eval-form
                          :form (list 'ns ns-name)}))


(defn- remove-temp-ns [eval-form ns-name]
  (middleware-evaluation {:middleware-name ::wrap-eval-in-temp-ns
                          :middleware-action ::removing-temp-ns
                          :eval-form eval-form
                          :form (list 'remove-ns (list 'quote ns-name))}))


(defn wrap-eval-in-temp-ns
  "Middleware that makes the evaluation take place in a temporary namespace."
  ([eval-ctxt]
   (wrap-eval-in-temp-ns eval-ctxt (gensym "temp_ns__")))
  ([eval-ctxt temp-ns]
   (fn [{:keys [eval-form] :as ctxt}]
     (let [res (do
                 (switch-to-temp-ns eval-form temp-ns)
                 (eval-ctxt ctxt))]
       (remove-temp-ns eval-form temp-ns)
       res))))


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
   (evaluate eval-form wrap-eval-forms forms)))

(comment
  (eval-forms '[(println *ns*)
                (ns foobar)
                (def inc* inc)
                (inc* 3)
                (println *ns*)])


  (contains? (into (sorted-set)
                   (comp
                     (map str)
                     (map keyword))
                   (all-ns))
             :foobar))


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
  - `eval-form`: a function a evaluates one form defaulting to `clojure.core/eval`."
  ([forms]
   (eval-forms-in-temp-ns eval forms))
  ([eval-form forms]
   (evaluate eval-form wrap-eval-forms-in-temp-ns forms)))


(comment
  ;; get loaded nss
  (into (sorted-set)
        (comp
          (map str)
          (map keyword))
        (all-ns))

  (eval-forms-in-temp-ns '[(+ 1 2 3)
                           (println *ns*)
                           (throw (ex-info "some msg" {:toto 1}))])

  (-> *e ex-data) ;; should contains faulty form
  (-> *e ex-cause ex-message)
  (-> *e ex-cause ex-data)) ; should contain original ex-data