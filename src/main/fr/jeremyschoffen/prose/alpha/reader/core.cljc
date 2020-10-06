(ns ^{:author "Jeremy Schoffen"
      :doc "
This namespaces provides a reader that combines our grammar and clojure's reader to turn a string of prose text into
data clojure can then evaluate.

The reader starts by parsing the text using our grammar giving a first representation,
then computes a *clojurized* version of the parse tree.

The different syntactic elements are processed as follows:
- text -> string
- clojure call -> itself
- symbol -> itself
- tag -> clojure fn call
- verbatim block -> string containing the verbatim block's content.
"}
  fr.jeremyschoffen.prose.alpha.reader.core
  (:require
    [edamame.core :as eda]
    [clojure.walk :as walk]
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


(defn read-string*
  "Wrapping of clojure(script)'s read-string function for use in our reader."
  [s]
  (try
    (eda/parse-string s *reader-options*)
    (catch #?@(:clj [Exception e] :cljs [js/Error e])
           (throw
             (ex-info "Reader failure."
                      {:type ::error/clojure-reader-error
                       :text s
                       :region *parse-region*
                       :failure e})))))


;;----------------------------------------------------------------------------------------------------------------------
;; Clojurizing
;;----------------------------------------------------------------------------------------------------------------------
(declare clojurize)


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


(defn add-type [x t]
  (vary-meta x assoc ::type t))


(defmulti clojurize* :tag)


(defmethod clojurize* :default [node]
  (throw (ex-info "Unknown parse result." {:tag node})))


(defmethod clojurize* :doc [node]
  (mapv clojurize (:content node)))


(defmethod clojurize* :symbol-use [node]
  (-> node :content first read-string*))


(defmethod clojurize* :clojure-call [node]
  (-> node :content clojurize-mixed))


(defmethod clojurize* :tag [node]
  (->> node
       :content
       (into [] (mapcat clojurize))
       seq))


(defmethod clojurize* :tag-unspliced [node]
  (->> node
       :content
       (into [] (map clojurize))
       seq))


(defmethod clojurize* :tag-name [node]
  (-> node :content first read-string* vector))


(defmethod clojurize* :tag-clj-arg [node]
  (add-type (-> node
                :content
                clojurize-mixed)
            :tag-clj-arg))


(defmethod clojurize* :tag-text-arg [node]
  (add-type (->> node
                 :content
                 (mapv clojurize))
            :tag-text-arg))


(defn- add-parse-region-meta [form region]
  (->> region
       (medley/map-keys #(-> % name keyword))
       (vary-meta form assoc ::parse-region)))


(defn clojurize
  "Function that turns a prose parse tree to data that clojure can eval.
  clojure form that are clojurized have parse info in their metadata."
  [form]
  (if (string? form)
    form
    (binding [*parse-region* (meta form)]
      (let [res (clojurize* form)]
        (cond-> res
                (not (string? res))
                (add-parse-region-meta *parse-region*))))))



(defn read-from-string* [text]
  (try
    (let [parsed (parse text)]
      (clojurize parsed))
    (catch #?@(:clj [Exception e] :cljs [js/Error e])
      (error/handle-read-error e))))


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


(comment
  (def ex1
    "Hello my name is ◊em{Jeremy}{Schoffen}.
     We can embed code ◊(+ 1 2 3).
     We can even embed tags in code:
     ◊(call ◊text{◊em{Me!}})

     Tags ins tags args:
     ◊toto[:arg1 ◊em{toto} :arg2 2 :arg3 \"arg 3\"].

     The craziest, we can embed ad nauseam:

     ◊(defn template [x]
        ◊div
        {
          the value x: ◊|x
          the value x++: ◊(inc x)
        })")
  (g/parser ex1)
  (read-from-string ex1)

  (read-from-string "◊div[:a \"stuff]\" :b 1]")
  (read-from-string "some text ◊(str \"aaa\"\")")

  (read-from-string "◊div{wanted to use the ◊\"}\" char}")
  (read-from-string "◊◊div{wanted to use the ◊\"}\" char}{in} [there]")
  (->> (read-from-string "◊◊div{wanted to use the ◊\"}\" char}{in} [there]")
       first
       (map meta))

  (read-from-string "◊[ 1 2 3 a]")
  (read-from-string "◊str◊{some str}"))
