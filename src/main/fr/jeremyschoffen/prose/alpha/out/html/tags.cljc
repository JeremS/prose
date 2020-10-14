(ns ^{:author "Jeremy Schoffen"
      :doc "
Api containing constructor functions for html tags.
"}
  fr.jeremyschoffen.prose.alpha.out.html.tags
  (:refer-clojure
    :exclude [map meta time var comment])
  (:require
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.html.tags.definition :as d :include-macros true]))


(d/def-all-tags)


(lib/def-xml-tag <> "The fragment tag.")


(defn dtd [name public-id system-id]
  {:type :dtd
   :data [name public-id system-id]})


(defn html5-dtd
  "The html 5 doctype tag."
  [& args]
  (dtd "html" nil nil))


(defn comment
  "A html comment."
  [& args]
  {:type :comment, :data (vec args)})
