(ns fr.jeremyschoffen.prose.alpha.eval.clojure-test
  (:require
    [clojure.test :refer :all]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval :include-macros true]))


(def program-1 '[(conj [1 2] 3) (+ 1 2 3)])

(def evaluation-1 (eval/eval-forms program-1))

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
    (eval/eval-forms faulty-program)
    (catch Exception e e)))


(deftest faulty
  (is (= {:prose.alpha.evaluation/env {:prose.alpha/env :clojure}
          :prose.alpha.evaluation/form faulty-form}
         (-> faulty-evaluation ex-data)))


  (is (= (-> faulty-evaluation ex-cause ex-data)
         {:some :expected-data})))

