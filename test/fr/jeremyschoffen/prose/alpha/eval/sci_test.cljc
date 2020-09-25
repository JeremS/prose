(ns fr.jeremyschoffen.prose.alpha.eval.sci-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
    [clojure.walk :as walk]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as sci-eval]
    [sci.core :as sci]))

;;----------------------------------------------------------------------------------------------------------------------
;; Evaluation from clojure
;;----------------------------------------------------------------------------------------------------------------------


(def my-eval (sci-eval/wrap-sci-bindings  sci-eval/eval-forms {sci/out *out*}))

(def program-1 '[(conj [1 2] 3) (+ 1 2 3)])

(def evaluation-1 (my-eval program-1))

(deftest p1
  (is (= evaluation-1
         [(conj [1 2] 3)
          (+ 1 2 3)])))

;;----------------------------------------------------------------------------------------------------------------------
(def faulty-form '(throw (ex-info "Expected error" {:some :expected-data})))
(def faulty-program ['(conj [1 2] 3)
                     faulty-form])

(def faulty-evaluation
  (try
    (my-eval faulty-program)
    (catch #?(:clj Exception :cljs js/Error) e e)))


(deftest faulty
  (is (= {:prose.alpha.evaluation/env {:prose.alpha.eval/env :clojure-sci},
          :prose.alpha.evaluation/form '(throw (ex-info "Expected error" {:some :expected-data}))}
         (-> faulty-evaluation ex-data)))


  (is (= #?(:clj (-> faulty-evaluation ex-cause ex-cause ex-data)
            :cljs (-> faulty-evaluation ex-cause ex-data))
         {:some :expected-data})))

;;----------------------------------------------------------------------------------------------------------------------
;; Evaluation from sci
;;----------------------------------------------------------------------------------------------------------------------
;; from clojure.template
(defn apply-template
  [argv expr values]
  (assert (vector? argv))
  (assert (every? symbol? argv))
  (walk/postwalk-replace (zipmap argv values) expr))


(defn make-sci-program [p]
  (apply-template '[program]
                  '[(require '[fr.jeremyschoffen.prose.alpha.eval.sci :as sci-eval])
                    (sci-eval/eval-forms-in-temp-ns 'program)]
                  [p]))

(def program-1' (make-sci-program program-1))
(def evaluation-1' (-> program-1'
                       sci-eval/eval-forms
                       last))

(deftest p1'
  (is (= evaluation-1'
         [(conj [1 2] 3)
          (+ 1 2 3)])))


(def faulty-program' (make-sci-program faulty-program))

(def faulty-evaluation'
  (try
    (sci-eval/eval-forms faulty-program')
    (catch #?(:clj Exception :cljs js/Error) e e)))


(deftest faulty'
  (is (= {:prose.alpha.evaluation/env #:prose.alpha.eval{:env :clojure-sci},
          :prose.alpha.evaluation/form '(sci-eval/eval-forms-in-temp-ns
                                          (quote [(conj [1 2] 3) (throw (ex-info "Expected error" {:some :expected-data}))]))}
         (-> faulty-evaluation' ex-data)))

  (is (= {:prose.alpha.evaluation/env {:prose.alpha.eval/env :sci},
          :prose.alpha.evaluation/form '(throw (ex-info "Expected error" {:some :expected-data}))}
         (-> faulty-evaluation' ex-cause ex-data (select-keys #{:prose.alpha.evaluation/env
                                                                :prose.alpha.evaluation/form}))))


  (is (= #?(:clj (-> faulty-evaluation' ex-cause ex-cause ex-cause ex-data)
            :cljs (-> faulty-evaluation' ex-cause ex-cause ex-data))
         {:some :expected-data})))