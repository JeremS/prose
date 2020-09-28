(ns fr.jeremyschoffen.prose.alpha.eval.sci-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is]]
       :cljs [cljs.test :refer-macros [deftest testing is]])
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
    (catch #?@(:clj [Exception e] :cljs [js/Error e]) e)))


(deftest faulty
  (is (= {:prose.alpha.evaluation/env {:prose.alpha/env :clojure-sci},
          :prose.alpha.evaluation/form '(throw (ex-info "Expected error" {:some :expected-data}))}
         (-> faulty-evaluation ex-data)))


  (is (= #?(:clj (-> faulty-evaluation ex-cause ex-cause ex-data)
            :cljs (-> faulty-evaluation ex-cause ex-data))
         {:some :expected-data})))
