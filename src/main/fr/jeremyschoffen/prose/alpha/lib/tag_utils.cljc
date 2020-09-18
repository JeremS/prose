(ns fr.jeremyschoffen.prose.alpha.lib.tag-utils
  (:require
    #?@(:clj [[clojure.spec.alpha :as s]
              [clojure.core.specs.alpha :as core-specs]]
        :cljs [[cljs.spec.alpha :as s :include-macros true]
               [cljs.core.specs.alpha :as core-specs]])
    [meander.epsilon :as m :include-macros true]))



(defn conform-or-throw [spec v]
  (let [res (s/conform spec v)]
    (when (s/invalid? res)
      (throw (ex-info (str "Invalid input, didn't conform to spec " spec ".")
                      (s/explain-data spec v))))
    res))


;;----------------------------------------------------------------------------------------------------------------------
;; Specs describing reader data for tags
;;----------------------------------------------------------------------------------------------------------------------
(defn- has-k-v? [m k v]
  (= (get m k) v))


(defn  clj-arg? [m]
  (and (map? m)
       (has-k-v? m :tag :tag-args-clj)))


(defn  text-arg? [m]
  (and (map? m)
       (has-k-v? m :tag :tag-args-txt)))


(s/def ::tag-clj-arg clj-arg?)
(s/def ::tag-txt-arg text-arg?)


;;----------------------------------------------------------------------------------------------------------------------
;; Xml like tags definition helpers
;;----------------------------------------------------------------------------------------------------------------------
(s/def ::html-like-tag-args (s/cat :attrs (s/? ::tag-clj-arg)
                                   :content (s/? ::tag-txt-arg)))


(defn- args->map [args]
  (apply hash-map args))


(defn make-xml-tag [name attrs content]
  {:tag name
   :attrs attrs
   :content content})


(defn xml-tag-args->tag [n args]
  (let [parsed (conform-or-throw ::html-like-tag-args args)]
    (make-xml-tag n
                  (or (some-> parsed :attrs :content args->map) {})
                  (-> parsed :content :content vec))))


(s/def ::def-xml-tag-args
  (s/cat :name symbol?
         :docstring (s/? string?)
         :keyword-name (s/? keyword?)))


(defmacro def-xml-tag
  "Define a function intended to be used in a textp document as a tag. For instance:
    ```clojure
    (ns my-ns
      (:require [fr.jeremyschoffen.textp.alpha.lib.core :refer [def-xml-tag]]))

    (def-xml-tag div \"The div tag\")
    ```

    can be used in a textp document:
    ```text
    ◊(require '[my-ns :refer [div]])◊
    Some text.
    ◊div[:class \"blue\"] {some text in the div.}
    ```

    When eventually read end eval-ed this div function will return something like:
    ```clojure
    {:tag :div
     :attrs {:class \"blue\"}
     :content [\"some text in the div.\"]}
    ```

    Args:
    - `name`: a symbol, name of the function/tag
    - `docstring`: a string
    - `keyword-name`: a keyword, the keyword name of the tag in the resulting map.
      Allows for:
      ```clojure
      (def-xml-tag html-meta :meta)
      ```
      instead of naming the tag/function `meta` and having to exclude `clojure.core/meta` from the ns in which
      the tag is defined.
    "
  {:arglists '([name docstring? keyword-name?])}
  [& args]
  (let [{:keys [name docstring keyword-name]
         :or {docstring ""
              keyword-name (keyword name)}} (conform-or-throw ::def-xml-tag-args args)]
    `(defn ~name
       ~docstring
       [& args#]
       (xml-tag-args->tag ~keyword-name args#))))


;;----------------------------------------------------------------------------------------------------------------------
;; Utilities helping the in the definition of functions to be employed in tag syntax.
;;----------------------------------------------------------------------------------------------------------------------

(s/def ::tag-fn-args (s/cat :arg (s/? ::tag-clj-arg)))


(defn clj-fn->tag-fn
  "Turn a classic clojure function into a function that can be used in tag form. The arguments
  of the new function will be passed in a clojure tag argument.
  For instance defining the following:
  ```clojure
  (defn add [x y]
    (+ x y))

  (def add-tag (clj-fn->tag-fn add))
  ```

 allows us to do this in textp documents:
 ```text
 ◊add-tag[1 2] instead of ◊(add 1 2)◊.
 ```"
  [f]
  (fn [& tag-args]
    (let [args (-> tag-args
                   (->> (conform-or-throw ::tag-fn-args))
                   (get-in [:arg :content]))]
      (apply f args))))


(defn- conform-defn-args [args]
  (s/conform ::core-specs/defn-args args))


(defn- unform-defn-args [args]
  (s/unform ::core-specs/defn-args args))


(defn- unform-artity-1 [params+body]
  (m/rewrite (s/unform ::core-specs/params+body params+body)
    (?params & ?body)
    (fn (m/app vec ?params) & ?body)))


(defn- unform-artity-n [param+bodies]
  (m/rewrite (map #(s/unform ::core-specs/params+body %) param+bodies)
    ((!params & !rest) ...)
    (fn . ((m/app vec !params) & !rest) ...)))


(defn- fn-tail->fn-form [fn-tail]
  (m/match fn-tail
           [:arity-1 ?param+body]
           {:fn-form (unform-artity-1 ?param+body)}

           [:arity-n {:bodies ?param+bodies
                             :attr-map ?attr-map}]
           {:fn-form (unform-artity-n ?param+bodies)
            :attr-map ?attr-map}))


(defn- parse-defn [fn-args]
  (let [conformed (conform-defn-args fn-args)]
    (update conformed :fn-tail fn-tail->fn-form)))


(defn- make-base-defn [parsed]
  (let [{:keys [meta]
         {:keys [attr-map]} :fn-tail} parsed]
    (-> parsed
        (dissoc :fn-tail)
        (cond-> (or meta attr-map)
                (update :meta merge attr-map))
        unform-defn-args)))


(defmacro def-tag-fn
  "Define a function that will be used in tag form in a text document.
    Similar to [[textp.lib.core/clj-fn->tag-fn]].

    You can define the function:
    ```clojure
    (def-tag-fn add [x y]
      (+ x y))
    ```

    to be used this way:
    ```text
    Some text then ◊add[1 2].
    ```

    This call would be equivalent to:
    ```clojure
    (add {:tag :tag-args-clj
          :content [1 2]})
    ```"
  {:arglists '([name doc-string? attr-map? [params*] prepost-map? body]
               [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])}
  [& args]
  (let [parsed (parse-defn args)
        base (make-base-defn parsed)
        fn-form (get-in parsed [:fn-tail :fn-form])]
    `(let [f# (clj-fn->tag-fn ~fn-form)]
       (defn ~@base [& args#]
         (apply f# args#)))))


(defn fragment
  "A tag that can be used a as fragment."
  [content]
  (let [content (-> content
                    (->> (conform-or-throw ::tag-txt-arg))
                    :content)]
    (make-xml-tag ::fragment {} content)))


(def fragment-kw ::fragment)