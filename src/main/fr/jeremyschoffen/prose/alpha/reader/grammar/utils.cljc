(ns fr.jeremyschoffen.prose.alpha.reader.grammar.utils
  (:require
    [medley.core :as medley]
    [instaparse.combinators :as instac]
    [lambdaisland.regal :as regal]))


(defmacro def-regex
  "Macro used to short hand:
    ```clojure
    (def a-regex (make-regex \"a regal expression\"))
    ```
    into
    ```clojure
    (def-regex a-regex \"a regal expression\")
    ```
    "
  ([n xeger-expr]
   `(def-regex ~n "" ~xeger-expr))
  ([n doc xeger-expr]
   `(def ~n ~doc (regal/regex ~xeger-expr))))


(defmacro make-lexer
  "Make a sequence of named regular expression into a instaparse map of named regex rules."
  [& regexes]
  `(into {}
         ~(vec (for [r regexes]
                 (let [kw (-> r name keyword)]
                   `[~kw (instac/regexp ~r)])))))


(defmethod regal/-regal->ir [:*? :common] [[_ & rs] opts]
  (regal/quantifier->ir "*?" rs opts))


(defn enclosed-text
  "Text found between balanced delimiters."
  [& forbidden]
  [:* (into [:not] forbidden)])


#_{:clj-kondo/ignore [:unresolved-var]}
(defn enclosed
  "Make a grammatical describing text enclosed in balanced delimiters."
  [open-rule close-rule & enclosed]
  (instac/cat
    open-rule
    (instac/star (apply instac/alt enclosed))
    close-rule))


#_{:clj-kondo/ignore [:unresolved-var]}
(defn hide-tags-all
  "Hide all tags of a grammar."
  [g]
  (medley/map-vals instac/hide-tag g))


(defn hide-tags
  "Selectively hide the tags of a grammar."
  [g tag-names]
  (-> g
      (select-keys tag-names)
      hide-tags-all
      (->> (merge g))))


#_{:clj-kondo/ignore [:unresolved-var]}
(defn hide-all
  "Hide all productions of a grammar."
  [g]
  (medley/map-vals instac/hide g))


(defn hide
  "Selectively hide the productions of a grammar."
  [g tag-names]
  (-> g
      (select-keys tag-names)
      hide-all
      (->> (merge g))))

