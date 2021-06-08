(ns ^{:author "Jeremy Schoffen"
      :doc "
Specialization of the generic compiler from [[fr.jeremyschoffen.prose.alpha.compilation.core]]
to compile to html.
"}
  fr.jeremyschoffen.prose.alpha.out.html.compiler
  (:require
    [fr.jeremyschoffen.prose.alpha.compilation.core :as common :refer [emit! emit-seq!]]
    [fr.jeremyschoffen.prose.alpha.out.html.tags :as tags]))



;; Generaly inspired by https://github.com/cgrand/enlive/blob/master/src/net/cgrand/enlive_html.clj

;;----------------------------------------------------------------------------------------------------------------------
;; arround https://github.com/cgrand/enlive/blob/master/src/net/cgrand/enlive_html.clj#L122
(defn xml-str
  "Like clojure.core/str but escapes < > and &."
  [x]
  (-> x str (.replace "&" "&amp;") (.replace "<" "&lt;") (.replace ">" "&gt;")))

(defn attr-str
  "Like clojure.core/str but escapes < > & and \"."
  [x]
  (-> x str (.replace "&" "&amp;") (.replace "<" "&lt;") (.replace ">" "&gt;") (.replace "\"" "&quot;")))
;;----------------------------------------------------------------------------------------------------------------------


;;----------------------------------------------------------------------------------------------------------------------
;; inspired by https://github.com/cgrand/enlive/blob/master/src/net/cgrand/enlive_html.clj#L175
(defn emit-comment!
  "Emit an html comment passed in map form."
  [node]
  (emit! "<!--")
  (apply emit! (map xml-str (:data node)))
  (emit! "-->"))


(defn emit-dtd!
  "Emit a dtd passed in map form."
  [{[name public-id system-id] :data}]
  (emit!
    (cond
      public-id
      (str "<!DOCTYPE " name " PUBLIC \"" public-id "\"\n    \"" system-id "\">\n")
      system-id
      (str "<!DOCTYPE " name " SYSTEM \"" system-id "\">\n")
      :else
      (str "<!DOCTYPE " name ">\n"))))
;;----------------------------------------------------------------------------------------------------------------------


;;----------------------------------------------------------------------------------------------------------------------
;; low level api
;;----------------------------------------------------------------------------------------------------------------------
(def self-closing-tags
  "Html tag names of tags that are self closing."
  #{:area :base :basefont :br :hr :input :img :link :meta})


(defn- named? [x]
  (or (symbol x) (keyword? x)))


(def ^:private destructure-named (juxt namespace name))


(defn- named->name-str [n]
  (let [[ns name] (destructure-named n)]
    (if ns
      (str ns ":" name)
      name)))


(defn name-str
  "Return a string representation of a name (tag or attribute) in html."
  [n]
  (cond
    (string? n) n
    (named? n) (named->name-str n)
    :else (throw (ex-info (str "Can't make an html name from: " n) {}))))


(defn- emit-attrs! [attrs]
  (doseq [[k v] attrs]
    (emit! \space (name-str k) \= \" (attr-str v) \")))


(defn- emit-content&close-tag! [tag content]
  (let [tag-name-str (name-str tag)]
    (if (seq content)
      (do (emit! ">")
          (emit-seq! content)
          (emit! "</"tag-name-str ">"))
      (if (contains? self-closing-tags tag)
        (emit! " />")
        (emit! "></" tag-name-str ">")))))


(defn emit-tag!
  "Emits a html tag which is a map with the keys `:tag`, `:attrs` and `:content`."
  [{:keys [tag attrs content]}]
  (let [tag-name (name-str tag)]
    (emit! "<" tag-name)
    (emit-attrs! attrs)
    (emit-content&close-tag! tag content)))


;;----------------------------------------------------------------------------------------------------------------------
;; Special cases
;;----------------------------------------------------------------------------------------------------------------------
(defmethod common/emit-special! [::html :dtd] [x]
  (emit-dtd! x))


(defmethod common/emit-special! [::html :comment] [x]
  (emit-comment! x))


;;----------------------------------------------------------------------------------------------------------------------
;; implementation
;;----------------------------------------------------------------------------------------------------------------------
(derive ::html ::common/default)


(defn emit-str!
  "Default emit-str for the HTML compiler. Uses [[xml-str]] to escaped characters."
  [x]
  (emit! (xml-str x)))


(def implementation
  "Html implementation of our generic compiler, this is meant to a binding to
  [[fr.jeremyschoffen.prose.alpha.compilation.core]]."
  (assoc common/*implementation*
    :name ::html
    :default-emit-str! emit-str!
    :default-emit-tag! emit-tag!))


(defn compile!
  "Compile a document (in data form) into html text."
  [doc]
  (common/text-environment
    (common/with-implementation implementation
      (common/emit-doc! doc))))


(comment
  (compile! [(tags/html5-dtd)  "toto" "titi" (tags/comment "<some--> " "comment") {:tag :div :content ["content"]}])
  (compile! (tags/div (tags/<> "<text"))))
