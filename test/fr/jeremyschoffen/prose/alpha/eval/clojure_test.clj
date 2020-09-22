(ns fr.jeremyschoffen.prose.alpha.eval.clojure-test
  (:require
    [clojure.test :refer :all]
    [fr.jeremyschoffen.prose.alpha.eval.common :as eval :include-macros true]))


(deftest evil
  (testing "normal eval"
    (let [program '[(conj [1 2] 3)
                    (+ 1 2 3)]
          res (eval/eval-forms-in-temp-ns program)]

      (is (= res
             [(conj [1 2] 3)
              (+ 1 2 3)]))))


  (testing "Exception raised"
    (let [faulty-form '(throw (Exception. "Random exception"))
          faulty-program ['(conj [1 2] 3)
                          faulty-form]
          res (try
                (eval/eval-forms-in-temp-ns faulty-program)
                (catch Exception e e))]


      (is (= (-> res ex-data)
             {:form faulty-form})))))
