(ns fr.jeremyschoffen.prose.alpha.document.lib-test
  (:require
    #?(:clj [clojure.test :refer [deftest testing is are]]
       :cljs [cljs.test :refer-macros [deftest testing is are]])
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib :include-macros true]))


(lib/def-xml-tag div :div)
(lib/def-xml-tag link)

(deftest tag-constructor
  (are [x y] (= x y)
             (div)
             {:tag :div :type :tag}

             (div {:class "toto"} "content")
             {:tag :div, :attrs {:class "toto"}, :content ["content"] :type :tag}

             (div {:class "toto"})
             {:tag :div, :attrs {:class "toto"} :type :tag}

             (div "content")
             {:tag :div  :content ["content"] :type :tag}

             (link {:rel "stylesheet" :type "text/css" :href "some/path"})
             {:tag :link :attrs {:rel "stylesheet" :type "text/css" :href "some/path"} :type :tag}))

