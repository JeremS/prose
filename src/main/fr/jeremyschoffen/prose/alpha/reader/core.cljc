(ns ^{:author "Jeremy Schoffen"
      :doc "
      This namespaces provides a reader that combines our grammar and clojure's reader to turn a string of text into
      data clojure can then evaluate.

      ## Reader results
      The reader starts by parsing the text using our grammar then returns a *clojurized* version of the parse tree.

      The different syntactic elements are processed as follows:

      - text -> string
      - tag -> clojure fn call
      - verbatim block -> string containing the verbatim block's content.
      - comments -> empty string or special map containing the comment depending on
        [[textp.reader.alpha.core/*keep-comments*]]
      - embedded clojure -> drop in clojure code or a map containing the code depending on
        [[textp.reader.alpha.core/*wrap-embedded*]]

      ## Special maps
      The reader can wrap comment/embedded clojure in maps if indicated to. These maps have 2 keys:
      - `type`: a marker explaining the kind of special value the map represents
      - `data`: the actual value being wrapped, the content of a comment or the embedded clojure code.

      This model is consistent with the way [https://github.com/cgrand/enlive](enlive) treats dtd elements
      for instance. This may allow for uniform processing when generating html for instance.
      "}
  fr.jeremyschoffen.prose.alpha.reader.core
  (:refer-clojure :exclude [comment])
  (:require
    [edamame.core :as eda]
    [clojure.walk :as walk]
    [net.cgrand.macrovich :as macro :include-macros true]
    [instaparse.core :as insta]
    [medley.core :as medley]

    [fr.jeremyschoffen.prose.alpha.reader.grammar :as g]
    [fr.jeremyschoffen.prose.alpha.reader.core.error :as error]))


;;----------------------------------------------------------------------------------------------------------------------
;; Parsing and reading
;;----------------------------------------------------------------------------------------------------------------------
(defn parse
  "Wrapper around the parser from [[textp.reader.grammar]] adding error handling."
  [text]
  (let [parsed (g/parser text)]
    (when (insta/failure? parsed)
      (throw (ex-info "Parser failure."
                      {:type ::error/grammar-error
                       :failure (insta/get-failure parsed)})))
    (insta/add-line-and-column-info-to-metadata text parsed)))


(def ^:dynamic *parse-region*
  "Stores the parse regions given by instaparse when clojurizing the parse tree."
  {})


(def ^:dynamic *reader-options* {:deref true
                                 :fn true
                                 :quote true
                                 :read-eval false
                                 :regex true
                                 :syntax-quote true
                                 :var true
                                 :read-cond :preserve})


(macro/replace
  #?(:clj {}
     :cljs {Exception js/Error})
  (defn read-string*
    "Wrapping of clojure(script)'s read-string function for use in our reader."
    [s]
    (try
      (eda/parse-string s *reader-options*)
      (catch Exception e
        (throw
          (ex-info "Reader failure."
                   {:type ::error/clojure-reader-error
                    :text s
                    :region *parse-region*
                    :failure e}))))))

;;----------------------------------------------------------------------------------------------------------------------
;; Clojurizing
;;----------------------------------------------------------------------------------------------------------------------
(declare clojurize)


(defmulti clojurize* :tag)


(defn extract-tags [content]
  "Replaces the tags by generated unique symbols and creates a mapping from
  those symbols to the replaced tag data."
  (let [env (volatile! (transient {}))
        form (volatile! (transient []))]

    (doseq [v content]
      (if (string? v)
        (vswap! form conj! v)
        (let [sym (gensym "tag")]
          (vswap! env assoc! sym v)
          (vswap! form conj! (str " " sym " ")))))

    {:env (-> env deref persistent!)
     :form (->  form
                deref
                persistent!
                (->> (apply str)))}))


(defn inject-clojurized-tags
  "Walks the clojurized block and replaces placeholder symbols by the clojurized content."
  [form env]
  (walk/prewalk (fn [v]
                  (if-let [t (and (symbol? v)
                                  (get env v))]
                    (clojurize t)
                    v))
                form))


(defn clojurize-mixed
  "The basic content of an embedded code block is a sequence of strings and tags. These tags can't be read by
  the clojure reader.

  To turn that block into clojure data, the trick is to replace the tags by place-holder strings that will be read as
  symbols. We can then use the clojure(script) reader on the result. Next we walk the code that's now data and replace
  those symbols with the clojurized tags."
  [content]
  (let [{:keys [env form]} (extract-tags content)
        form (read-string* form)]
    (inject-clojurized-tags form env)))


(defmethod clojurize* :doc [node]
  (mapv clojurize (:content node)))


(defmethod clojurize* :verbatim [form]
  (-> form :content first))


(defmethod clojurize* :comment [_] "")


(defmethod clojurize* :embedded-value [form]
  (-> form :content first read-string*))


(defmethod clojurize* :embedded-code [form]
  (clojurize-mixed (:content form)))


(defmethod clojurize* :tag [form]
  (->> form
       :content
       (into [] (mapcat clojurize))
       seq))


(defmethod clojurize* :tag-name [form]
  (-> form :content first read-string* vector))


(defmethod clojurize* :tag-args-txt [form]
  (->> form
       :content
       (mapv clojurize)))


(defmethod clojurize* :tag-args-clj [form]
  (-> form
      :content
      clojurize-mixed))


(defn- add-parse-region-meta [form region]
  (->> region
       (medley/map-keys #(-> % name keyword))
       (vary-meta form assoc ::parse-region)))


(defn clojurize
  "Function that turns a textp parse tree to data that clojure can eval.
  clojure form that are clojurized have parse info in their metadata."
  [form]
  (if (string? form)
    form
    (binding [*parse-region* (meta form)]
      (let [res (clojurize* form)]
        (cond-> res
                (not (string? res))
                (add-parse-region-meta *parse-region*))))))


(macro/replace
  #?(:clj {}
     :cljs {Exception js/Error})
  (defn read-from-string* [text]
    (try
      (let [parsed (parse text)]
        (clojurize parsed))
      (catch Exception e
        (error/handle-read-error e)))))


(defn read-from-string
  "
  Args:
  - `text`: string we want to read
  - `opts`: a :map specifying options

  Options:
  - `:reader-options`: The options to pass the clojure reader, it's the map that will be passed to
    [[edamame.core/parse-string]]. By default every basic option is allowed except `:read-eval`.
  "
  ([text]
   (read-from-string* text))
  ([text opts]
   (binding [*reader-options* (get opts :reader-options *reader-options*)]
     (read-from-string* text))))


(defn form->text
  "Given a form and the original text, finds the part of the text that read as this form."
  [form original]
  (if (string? form)
    form
    (let [{:keys [start-index end-index]} (-> form meta ::parse-region)]
      (subs original start-index end-index))))


(clojure.core/comment
  (read-from-string "◊/com/")
  (def ex1
    "

 ◊h1{Addition}

 The addition adds numbers together...

 ◊(+ 1 2 3)◊

 The Addition is associative:

 ◊ul{
   ◊li{◊(+ 1 (+ 2 3))◊}
   ◊li{◊(+ (+ 1 2) 3)◊}
 }

 are equivalent.

 ◊div{\\} \\1}")
  (parse ex1)

  (-> ex1
      (read-from-string))
  (-> ex1
      (read-from-string)
      second
      (form->text ex1))
  (-> ex1
      (read-from-string)
      (->> (drop 3))
      first
      (form->text ex1))

  (def ex2
    "Hello my name is ◊em{Some}{Name}.
     We can embed code ◊(+ 1 2 3)◊.
     We can even embed tags in code:
     ◊(call ◊text{◊em{Me!}})◊

     Tags ins tags args:
     ◊toto[:arg1 ◊em{toto} :arg2 2 :arg3 \"arg 3\"].

     The craziest, we can embed ad nauseam:

     ◊(defn template [x]
        ◊div[:bonkers ◊div{some text}]
        {
          the value x: ◊|x|◊
          the value x++: ◊(inc x)◊
        })◊

     ◊defn [[x]] {◊div}")

  (read-from-string ex2)

  (parse ex2)
  (-> (read-from-string ex2)
      second
      first
      meta))


