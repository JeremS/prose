(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing several tools to use inside or outside of prose document.
"}
  fr.jeremyschoffen.prose.alpha.document.lib
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s :include-macros true])

    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]))


;;----------------------------------------------------------------------------------------------------------------------
;; Textually silent versions of clojure definitions
;;----------------------------------------------------------------------------------------------------------------------
(defmacro def-s
  "Same as `def` excpet that it returns and empty string."
  [& args]
  `(do
     (def ~@args)
     ""))


(defmacro defn-s [& args]
  "Same as `defn` excpet that it returns and empty string."
  `(do
     (defn ~@args)
     ""))


(defmacro defmacro-s
  "Same as `defmacro` excpet that it returns and empty string."
  [& args]
  `(do
     (defmacro ~@args)
     ""))


;;----------------------------------------------------------------------------------------------------------------------
;; Tag utils
;;----------------------------------------------------------------------------------------------------------------------
(defn conform-or-throw
  "Conforms the value `v` using the spec `spec` and throws an `ex-info` if `v` is invalid."
  [spec v]
  (let [res (s/conform spec v)]
    (when (s/invalid? res)
      (throw (ex-info (str "Invalid input, didn't conform to spec " spec ".")
                      (s/explain-data spec v))))
    res))


(defn tag-type
  "Returns the type of a tag `x` or nil isn't a map or doesn't have a type."
  [x]
  (when (map? x)
    (:type x)))


(defn special?
  "Determine if `x` is a special tag."
  [x]
  (when-let [t (tag-type x)]
    (not= :tag t)))


(defn tag?
  "Determine if `x` is a normal tag."
  [x]
  (when-let [t (tag-type x)]
    (= :tag t)))


(s/def ::xml-tag (s/cat :tag keyword?
                        :attrs (s/? (every-pred map?
                                                (complement (some-fn special? tag?))))
                        :content (s/* any?)))

(defn xml-tag
  "Constructor of normal tags.

  args:
  - `name`: a keyword giving the tag his name (:div, :my-tag, etc...)
  - `attrs`: a map of attributes
  - `content`: the content of the tag, strings, other (special)tags or sequences of both.

  Returns a map similar to:
  ```clojure
  {:type :tag
   :tag name
   :attrs attrs?
   :content content}
  ```
  "
  {:arglists '([name attrs? & content])}
  [& args]
  (->  (conform-or-throw ::xml-tag args)
       (assoc :type :tag)))


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


(comment ; clj only
  (macroexpand-1 '(def-xml-tag div))

  (def-xml-tag div)


  (div)
  (div {})
  (div "content")
  (div {} "content")
  (div (div) (div) (div)))


(defmacro def-xml-tags [& tags]
  `(do
     ~@(for [t tags]
         (let [[sym kw] (if (vector? t) t [t])]
           (if kw
             `(def-xml-tag ~sym ~kw)
             `(def-xml-tag ~sym))))))


;;----------------------------------------------------------------------------------------------------------------------
;; Default tags
;;----------------------------------------------------------------------------------------------------------------------
(defn <>
  "Fragment tag whose content is meant to be spliced into its parent's content."
  [& content]
  (apply xml-tag :<> {} content))


;;----------------------------------------------------------------------------------------------------------------------
;; Includes from inside documents
;;----------------------------------------------------------------------------------------------------------------------
(defn get-env
  "Get the evaluation environment or the value for one of its keys."
  ([]
   (eval-common/get-env))
  ([k]
   (eval-common/get-env k)))

(defn get-input
  "Get the input value from the evaluation environment."
  []
  (get-env :prose.alpha.document/input))


(defn get-slurp-doc
  "Get the slurping function from the evaluation environment."
  []
  (get-env :prose.alpha.document/slurp-doc))


(defn get-read-doc
  "Get the reading function from the evaluation environment."
  []
  (get-env :prose.alpha.document/read-doc))


(defn get-eval-doc
  "Get the eval-forms function from the evaluation environment."
  []
  (get-env :prose.alpha.document/eval-forms))



(defn- load* [load-doc {path :path
                        error-msg :error-msg
                        :as ctxt}]
  (try
    (eval-common/bind-env {:prose.alpha.document/path path}
                          (load-doc path))
    (catch #?@(:clj [Exception e] :cljs [js/Error e])
           (throw (ex-info error-msg
                           (dissoc ctxt :error-msg)
                           e)))))


(defmacro insert-doc
  "Insert the slurped and read content of another document."
  [path]
  (apply <>
         (load* (comp (get-read-doc)
                      (get-slurp-doc))
                {:path path
                 :form &form
                 :error-msg "Error inserting doc."})))


(defn require-doc
  "Insert the slurped and read and evaluated content of another document."
  [path]
  (apply <> (load* (comp (get-eval-doc)
                         (get-read-doc)
                         (get-slurp-doc))
                   {:path path
                    :error-msg "Error requiring doc."})))



(comment
  (eval-common/bind-env {:prose.alpha.document/input {:some :input}}
                        (eval-common/eval-forms-in-temp-ns
                          '[(require '[fr.jeremyschoffen.prose.alpha.document.lib :refer [get-input]])
                            (get-input)]))

  (into (sorted-set)
        (comp
          (map str)
          (map keyword))
        (all-ns))

  (require '[clojure.java.io :as io])
  (require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])

  (def load-doc (fn [path]
                  (-> path
                      io/resource
                      slurp
                      reader/read-from-string)))

  (eval-common/bind-env {:prose.alpha.document/load-doc load-doc
                         :prose.alpha.document/eval-doc eval-common/eval-forms-in-temp-ns}
                        (-> "complex-doc/master.prose"
                            load-doc
                            eval-common/eval-forms-in-temp-ns)))




