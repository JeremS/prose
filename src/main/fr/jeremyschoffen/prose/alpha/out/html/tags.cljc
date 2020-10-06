(ns fr.jeremyschoffen.prose.alpha.out.html.tags
  (:refer-clojure
    :exclude [map meta time var comment])
  (:require
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.html.tags.definition :as d :include-macros true]))


(d/def-all-tags)


(lib/def-xml-tag <> "The fragment tags.")


(defn dtd [name public-id system-id]
  {:type :dtd
   :data [name public-id system-id]})


(defn html5-dtd [& args]
  (dtd "html" nil nil))


(defn comment [& args]
  {:type :comment, :data (vec args)})
