(ns fr.jeremyschoffen.prose.alpha.lib.tag-utils-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.lib.tags :as lib :include-macros true]))


(lib/def-xml-tag div :div)

(deftest tag-constructor
  (are [x y] (= x y)
    (div)
    {:tag :div}

    (div {:class "toto"} "content")
    {:tag :div, :attrs {:class "toto"}, :content ["content"]}

    (div {:class "toto"})
    {:tag :div, :attrs {:class "toto"}}

    (div "content")
    {:tag :div  :content ["content"]}))
