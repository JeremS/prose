(ns fr.jeremyschoffen.prose.alpha.lib.tag-utils-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.lib.tag-utils :as lib :include-macros true]))


(lib/def-xml-tag div :div)

(deftest tag-constructor
  (are [x y] (= x y)
    (div)
    {:tag :div  :attrs {} :content []}

    (div {:tag :tag-args-clj, :content [:class "toto"]}
         {:tag :tag-args-txt, :content ["content"]})
    {:tag :div, :attrs {:class "toto"}, :content ["content"]}

    (div {:tag :tag-args-clj, :content [:class "toto"]})
    {:tag :div, :attrs {:class "toto"} :content []}

    (div {:tag :tag-args-txt, :content ["content"]})
    {:tag :div  :attrs {} :content ["content"]})

  (are [x] (thrown? #?(:clj Exception
                       :cljs js/Error) x)
    (div {:tag :tag-args-txt, :content ["content"]}
         {:tag :tag-args-txt, :content ["content"]}
         {:tag :tag-args-clj, :content [:class "toto"]})

    (div {:tag :tag-args-txt, :content ["content"]}
         {:tag :tag-args-clj, :content [:class "toto"]}
         {:tag :tag-args-clj, :content [:class "toto"]})


    (div {:tag :tag-args-txt, :content ["content"]}
         {:tag :tag-args-clj, :content [:class "toto"]})))


(lib/def-tag-fn add [x y]
  (+ x y))


(lib/def-tag-fn example
  ([x] x)
  ([x y] [x y]))


(deftest tag-fn-cstr
  (testing "Adder"
    (is (= (add {:tag :tag-args-clj :content [1 2]}) 3))
    (are [x] (is (thrown? #?(:clj Exception
                             :cljs js/Error)
                          x))
      (add 1)
      (add 1 2)

      #?(:clj (add {:tag :tag-args-clj :content [1]}))
      #?(:clj (add {:tag :tag-args-clj :content [1 2 3]}))))

  (testing "example"
    (are [x y] (= x y)
      (example {:tag :tag-args-clj :content [1]}) 1
      (example {:tag :tag-args-clj :content [1 2]}) [1 2])))
