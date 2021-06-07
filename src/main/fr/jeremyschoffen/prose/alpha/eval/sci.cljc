(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing tools to facilitate the evaluation of documents using Sci.
"}
  fr.jeremyschoffen.prose.alpha.eval.sci
  (:require
    [medley.core :as medley]
    [sci.core :as sci :include-macros true]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common :include-macros true]))




(def sci-opt-features
  "Sci option map containing values for the `:features` key."
  {:features #?(:clj #{:clj}
                :cljs #{:cljs}
                :default #{})})


(def sci-opt-println
  "Sci option map containing values for the `:namespaces` key that allows in
  the clojurescript case to redefine sci's own `'clojure.core/println` to
  `cljs.core/println`. Using this map when creating sci contexts in clojure
  does nothing.

  This map is meant to be deep-merged into the options passed to [[init]]"
  {:namespaces #?(:clj {}
                  :cljs {'clojure.core {'println println}})})


(defn init
  "Create a sci evaluation context.

  Same as [[sci.core/init]] with the [[sci-opt-features]] pre-installed. "
  [opts]
  (->> opts
       (medley/deep-merge sci-opt-features)
       sci/init))


(comment
  (def env (init sci-opt-println))

  (sci/binding [sci/ns @sci/ns
                sci/out *out*]
    (doseq [f '[(println *ns*)
                (ns foobar)
                (def inc* inc)
                (println (inc* 3))
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
(defn fork-sci-ctxt
  "Alias for [[sci.core/fork]]"
  [sci-ctxt]
  (sci/fork sci-ctxt))


(defn sci-ctxt->sci-eval
  "Make an eval function from an sci context.

  The result is a function of one argument, a `form` to be evaluated by sci in
  the evaluation context `ctxt`."
  [ctxt]
  (fn [form]
    (sci/eval-form ctxt form)))


;;----------------------------------------------------------------------------------------------------------------------
;; Eval functions
;;----------------------------------------------------------------------------------------------------------------------
(defn eval-forms
  "Evaluate a sequence of forms with sci ensuring the the current namespace doesn't change after the evaluation."
  ([forms]
   (eval-forms (init nil) forms))
  ([sci-ctxt forms]
   (let [ef (sci-ctxt->sci-eval sci-ctxt)]
     (eval-common/bind-env {:prose.alpha/env :clojure-sci}
       (sci/binding [sci/ns @sci/ns]
         (eval-common/eval-forms ef forms))))))


(comment
  (sci/binding [sci/out *out*]
    (eval-forms
      (init sci-opt-println)
      '[(println *ns*)
        (ns foobar)
        (def inc* inc)
        (inc* 3)
        (println *ns*)]))

  (sci/binding [sci/out *out*]
    (eval-forms '[(println *ns*)
                  (ns foobar)
                  (def inc* inc)
                  (inc* 3)
                  (println *ns*)])))


(defn eval-forms-in-temp-ns
  "Evaluate a sequence of forms with sci in a temporary namespace."
  ([forms]
   (eval-forms-in-temp-ns (init nil) forms))
  ([sci-ctxt forms]
   (let [ef (sci-ctxt->sci-eval sci-ctxt)]
     (eval-common/bind-env {:prose.alpha/env :clojure-sci}
       (sci/binding [sci/ns @sci/ns]
         (eval-common/eval-forms-in-temp-ns ef forms))))))

(comment
  (sci/binding [sci/out *out*]
    (eval-forms-in-temp-ns
      (init sci-opt-println)
      '[(+ 1 2 3)
        (println *ns*)
        (throw (ex-info "some msg" {:toto 1}))]))

  (-> *e ex-data) ;; should contains faulty form
  (-> *e ex-cause  ex-message)
  (-> *e ex-cause ex-cause ex-data))
