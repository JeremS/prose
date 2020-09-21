(ns fr.jeremyschoffen.prose.alpha.lib.tags
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s :include-macros true]))
  #?(:cljs
     (:require-macros [fr.jeremyschoffen.prose.alpha.lib.tags :refer [def-xml-tag]])))

;;----------------------------------------------------------------------------------------------------------------------
;; Tag utils
;;----------------------------------------------------------------------------------------------------------------------
(defn conform-or-throw
  "Conforms the value `v`  using `spec` and throw if `v` is invalid."
  [spec v]
  (let [res (s/conform spec v)]
    (when (s/invalid? res)
      (throw (ex-info (str "Invalid input, didn't conform to spec " spec ".")
                      (s/explain-data spec v))))
    res))


(defn tag? [x]
  (and (map? x)
       (-> x meta :prose.alpha/tag)))


(s/def ::xml-tag (s/cat :tag keyword?
                        :attrs (s/? (every-pred map?
                                                (complement tag?)))
                        :content (s/* any?)))

(defn xml-tag [& args]
  (->  (conform-or-throw ::xml-tag args)
       (vary-meta assoc :prose.alpha/tag true)))


(s/def ::def-xml-tag
  (s/cat :name symbol?
         :docstring (s/? string?)
         :keyword-name (s/? keyword?)))

(defmacro def-xml-tag
  "Helper in making functions that construct tags.

  Args:
  - `name`: name of the function
  - `docstring?`: a docstring for the function
  - `keyword-name?`: the actual name of the tag, defaults to the keyword version of `name`"
  {:arglists '([name docstring? keyword-name?])}
  [& args]
  (let [{:keys [name docstring keyword-name]
         :or {docstring ""
              keyword-name (keyword name)}} (conform-or-throw ::def-xml-tag args)]
    `(defn ~name
       ~docstring
       [& args#]
       (apply xml-tag ~keyword-name args#))))


(comment
  (macroexpand-1 '(def-xml-tag div))

  (def-xml-tag div)

  (div)
  (div {})
  (div "content")
  (div {} "content")
  (div (div) (div) (div)))


;;----------------------------------------------------------------------------------------------------------------------
;; Default tags
;;----------------------------------------------------------------------------------------------------------------------

(def-xml-tag fragment
  "Tag whose content is meant to be spliced into its parent's content."
  :prose.alpha/fragment)

(def-xml-tag t
  "Tag whose role is to hold text."
  :prose.alpha/text)