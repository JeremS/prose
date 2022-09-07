(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing several tools to use inside or outside of prose document.
"}
  fr.jeremyschoffen.prose.alpha.document.lib
  (:require
    #?(:clj [clojure.spec.alpha :as s]
       :cljs [cljs.spec.alpha :as s :include-macros true])

    [fr.jeremyschoffen.prose.alpha.eval.common :as eval-common]
    [hyperfiddle.rcf :refer [tests]]))


;;----------------------------------------------------------------------------------------------------------------------
;; Textually silent versions of clojure definitions
;;----------------------------------------------------------------------------------------------------------------------
(defmacro def-s
  "Same as `def` except that it returns and empty string."
  [& args]
  `(do
     (def ~@args)
     ""))


(defmacro defn-s
  "Same as `defn` except that it returns and empty string."
  [& args]
  `(do
     (defn ~@args)
     ""))


(defmacro defmacro-s
  "Same as `defmacro` except that it returns and empty string."
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


(tests ; clj only
  (macroexpand-1 '(def-xml-tag div))

  (def-xml-tag div)


  (div) := {:type :tag :tag :div}
  (div {}) := {:tag :div :attrs {} :type :tag}
  (div "content") := {:tag :div :content ["content"] :type :tag}
  (div {} "content") := {:tag :div, :attrs {}, :content ["content"], :type :tag}
  (div (div) (div) (div)) := {:tag :div,
                              :content
                              [{:tag :div, :type :tag}
                               {:tag :div, :type :tag}
                               {:tag :div, :type :tag}],
                              :type :tag})


(defmacro def-xml-tags [& tags]
  `(do
     ~@(for [t tags]
         (let [[sym kw] (if (vector? t) t [t])]
           (if kw
             `(def-xml-tag ~sym ~kw)
             `(def-xml-tag ~sym))))))


(defn attr->set
  "Turn the string value of an html tag attribute into a set of strings
  (like the value class attribute)."
  [s]
  (set (re-seq #"\S+" s)))


(defn set->attr
  "Turn a set of strings/keywords into a string to be used as the value of an html tag attribute
  (like the class attribute)."
  [s]
  (apply str (interpose \space s)))


(defn add-classes [t classes]
  (let [current (-> t
                    (get-in [:attrs :class] "")
                    attr->set)
        new (into current (map name) classes)]
    (assoc-in t [:attrs :class] (set->attr new))))


(defn make-mixed-in
  "Make a tag constructor that injects some css classes.

  Args:
  - `cstr`: a tag constructor (defined with [[def-xml-tag]] for instance)
  - `classes`: a sequence of css classes names, can be string or keywords

  ex:
  (def container (make-mixed-in div [:container]))
  (container \"some text\")

  :=> {:tag :div :attrs {:classes \"container\"} :content \"some text\"}
  "
  [cstr classes]
  (let [{::keys [added-classes original-cstr]
         :or {added-classes #{}
              original-cstr cstr}}(meta cstr)
        classes (into added-classes classes)]
    (with-meta
      (comp #(add-classes % classes) original-cstr)
      {::added-classes classes
       ::original-cstr original-cstr})))


(tests
  "Testing the mixed in helpers."

  (attr->set "") := #{}
  (attr->set "container grid col-2") := #{"grid" "col-2" "container"}
  (set->attr (attr->set "container grid col-2")) := "grid col-2 container"


  (def-xml-tag div)
  (def container (make-mixed-in div #{:container}))
  (def container-grid (make-mixed-in container ["grid"]))

  (container {:class "toto"} "contained") := {:tag :div,
                                              :attrs {:class "toto container"},
                                              :content ["contained"],
                                              :type :tag}

  (container-grid {:class "titi"}"contaited 2") := {:tag :div,
                                                    :attrs {:class "grid titi container"},
                                                    :content ["contaited 2"],
                                                    :type :tag}

  (meta container) := {::added-classes #{:container}
                       ::original-cstr div}

  (meta container-grid) := {::added-classes #{"grid" :container},
                            ::original-cstr div})

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


(defn slurp-doc
  "Slurp a document using the function given by [[get-slurp-doc]]."
  [& args]
  (apply (get-slurp-doc) args))


(defn get-read-doc
  "Get the reading function from the evaluation environment."
  []
  (get-env :prose.alpha.document/read-doc))


(defn read-doc
  "Read a string using the function given by [[get-read-doc]]."
  [& args]
  (apply (get-read-doc) args))


(defn get-eval-doc
  "Get the eval-forms function from the evaluation environment."
  []
  (get-env :prose.alpha.document/eval-forms))


(defn eval-doc
  "Eval a document using the function given by [[get-eval-doc]]."
  [& args]
  (apply (get-eval-doc) args))


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
         (load* (comp read-doc slurp-doc)
                {:path path
                 :form &form
                 :error-msg "Error inserting doc."})))


(defn require-doc
  "Insert the slurped, read and evaluated content of another document."
  [path]
  (apply <> (load* (comp eval-doc read-doc slurp-doc)
                   {:path path
                    :error-msg "Error requiring doc."})))


#_{:clj-kondo/ignore [:unresolved-symbol]}
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


  (def slurp-doc (fn [path]
                  (-> path
                      io/resource
                      slurp)))

  (eval-common/bind-env {:prose.alpha.document/slurp-doc slurp-doc
                         :prose.alpha.document/read-doc reader/read-from-string
                         :prose.alpha.document/eval-forms eval-common/eval-forms-in-temp-ns}
                        (-> "complex-doc/master.prose"
                            load-doc
                            eval-common/eval-forms-in-temp-ns)))



