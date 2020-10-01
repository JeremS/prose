(ns ^{:author "Jeremy Schoffen"
      :doc "
# Prose's grammar.

The Grammar propsed here is heavily inspired by Pollen's.

We construct it using in 2 parts:
- a lexical part or lexer made of regular expressions.
- a set of grammatical rules tyring the lexer together into the grammar.

## The lexer.
Our lexer is made of regular expressions constructed with the
[[fr.jeremyschoffen.prose.alpha.reader.grammar.utils/def-regex]] macro. It uses the Regal library under the covers.

Then, to assemble these regexes into a grammar we use the
[[fr.jeremyschoffen.prose.alpha.reader.grammar.utils/make-lexer]] macro.

For instance we could construct the following 2 rules lexer:

```clojure
(def-regex number [:* :digit])

(def-regex word [:* [:class [\"a\" \"z\"]]])

(def lexer (make-lexer number word))

lexer
;=> {:number {:tag :regexp
              :regexp #\"\\d*\"}
     :word {:tag :regexp
            :regexp #\"[a-z]*\"}}
```

## The grammatical rules
Most of the grammatical rules are created using the ebnf notation as follows
```clojure
(def rules
  (instac/ebnf
    \"
    doc = (token <':'>)*
    token = (number | word)
    \"))

rules
;=>{:doc {:tag :star
          :parser {:tag :cat
                   :parsers ({:tag :nt :keyword :token}
                             {:tag :string :string \":\" :hide true})}}
           :token {:tag :alt
                   :parsers ({:tag :nt :keyword :number}
                             {:tag :nt :keyword :word})}}
```

## The combining trick
Now that we have both a lexer and and grammatical rules, we can simply merge them to have the full grammar.

We actually get a instparse parser this way:

````clojure
(def parser
  (insta/parser (merge lexer rules)
                :start :doc))

(parser \"abc:1:def:2:3:\")
;=> [:doc
      [:token [:word \"abc\"]]
      [:token [:number \"1\"]]
      [:token [:word \"def\"]]
      [:token [:number \"2\"]]
      [:token [:number \"3\"]]]
```

With the exception of some details, this is how this namespace is made."}
  fr.jeremyschoffen.prose.alpha.reader.grammar
  (:require
    [instaparse.core :as insta]
    [instaparse.combinators :as instac]

    [fr.jeremyschoffen.prose.alpha.reader.grammar.utils :as gu :include-macros true]))


;; ---------------------------------------------------------------------------------------------------------------------
;; Regal data composing our lexer
;; ---------------------------------------------------------------------------------------------------------------------
(def special \◊)
(def escape "\\")
(def pipe "|")

(def ns-end \/)
(def macro-reader-char \#)

(def open-paren "(")
(def close-paren ")")
(def open-brace "{")
(def close-brace "}")
(def open-bracket "[")
(def close-bracket "]")

;; classic delimiters
(def parens (str open-paren close-paren))
(def brackets (str open-bracket close-bracket))
(def braces (str open-brace close-brace))
(def double-quote "\"")

(def all-delimitors (sorted-set parens brackets braces double-quote))


(def anything [:class :whitespace :non-whitespace])


(def symbol-excluded-charset
  (into #{:whitespace special ns-end escape} all-delimitors))


(def symbol-regular-char
  "Characters that are always forbidden in symbol names:
  - spaces
  - diamond char since it starts another grammatical rule
  - delimitors: parens, brackets, braces  and double quotes.
  - `/` since it the special meaning of separating the namespace from the symbol name.
  - `.` since it has the special meaning of separating symbol names.
  - `\\` since it is reserved by clojure to identify a literal character."
  (into [:not] symbol-excluded-charset))


(def symbol-first-char
  "In the case of the first character of a symbol name, there are more forbidden chars:
  - digits aren't allowed as first character
  - the macro reader char `#` isn't allowed either."
  (into [:not]
        (conj symbol-excluded-charset
              :digit
              macro-reader-char)))


(def simple-symbol
  "Regex for the ns name of a symbol, parses dot separated names until
  a final name."
  [:cat symbol-first-char [:* symbol-regular-char]])


(def complex-symbol
  (let [ns-part [:cat [:capture simple-symbol] ns-end]
        name-part [:capture simple-symbol]]
    [:cat [:? ns-part] name-part]))

;; ---------------------------------------------------------------------------------------------------------------------
;; Lexer proper
;; ---------------------------------------------------------------------------------------------------------------------
(gu/def-regex special-char
  "The special character that denotes embedded code."
  special)


(gu/def-regex pipe-char
  "The pipe character found between the special character and a symbol when directly using a symbol in text."
  pipe)


(gu/def-regex escape-char
  "The escaping character"
  escape)


(gu/def-regex any-char
  "Any character whatsoever."
  anything)


(gu/def-regex plain-text
  "Text to be interpreted as plain text, neither clojure code, nor special blocks of text.
   Basically any character excluding diamond which have special meaning."
  [:* [:not special]])


(gu/def-regex verbatim-text
  "Text found in verbatim blocks."
  [:* [:not double-quote escape]])


(gu/def-regex clojure-string
  "Text found in clojure strings."
  [:cat
   double-quote
   [:* [:alt [:not double-quote escape]
        [:cat escape anything]]]
   double-quote])


(gu/def-regex clojure-call-text
  "Clojure text found in clojure calls (enclosed in parens)."
  (gu/enclosed-text special double-quote parens))


(gu/def-regex symbol-text
  "Regex used when parsing a symbol."
  complex-symbol)


(gu/def-regex tag-spaces
  "Spaces found in-between tag args."
  [:* :whitespace])


(gu/def-regex tag-clj-arg-text
  "Clojure text found in clojure arguments to tag fns (enclosed in brackets)."
  (gu/enclosed-text special double-quote brackets))


(gu/def-regex tag-text-arg-text
  "Regular text found in text arguments to tag fns (enclosed in braces)."
  (gu/enclosed-text special braces))


(def lexer*
  "Our lexer, an incomplete instaparse grammar in map form containing only regex rules."
  (gu/make-lexer
    special-char
    pipe-char
    escape-char
    any-char
    plain-text
    verbatim-text
    clojure-string
    clojure-call-text
    symbol-text
    tag-spaces
    tag-clj-arg-text
    tag-text-arg-text))


;; ---------------------------------------------------------------------------------------------------------------------
;; Grammar
;; ---------------------------------------------------------------------------------------------------------------------
(def enclosed-g
  "Grammatical describing text enclosed in balanced marker: quotes, parenthesis..;"
  {:verbatim-string (gu/enclosed (-> double-quote instac/string instac/hide)
                                 (-> double-quote instac/string instac/hide)
                                 (instac/nt :verbatim-text)
                                 (instac/nt :escaped-char))


   :paren-enclosed (gu/enclosed (instac/string open-paren) (instac/string close-paren)
                                (instac/nt :paren-enclosed)
                                (instac/nt :clojure-call-text)
                                (instac/nt :clojure-string)
                                (instac/nt :embedded))

   :bracket-enclosed (gu/enclosed (instac/string open-bracket) (instac/string close-bracket)
                                  (instac/nt :bracket-enclosed)
                                  (instac/nt :tag-clj-arg-text)
                                  (instac/nt :clojure-string)
                                  (instac/nt :embedded))

   :brace-enclosed (gu/enclosed (-> open-brace instac/string instac/hide)
                                (-> close-brace instac/string instac/hide)
                                (instac/nt :brace-enclosed)
                                (instac/nt :tag-text-arg-text)
                                (instac/nt :embedded))})


(def general-g
  "The general grammar, tying the lexer and the enclosed rules together with the top grammatical rules."
  (instac/ebnf
    "
  doc          = (plain-text | embedded)*
  embedded     = verbatim / symbol-use / clojure-call / tag

  verbatim     = special-char verbatim-string
  symbol-use   = special-char pipe-char symbol-text
  clojure-call = special-char paren-enclosed
  tag          = special-char tag-name  tag-args* !tag-args

  tag-name = symbol-text
  tag-args = tag-spaces (tag-clj-arg | tag-text-arg)
  tag-clj-arg = bracket-enclosed
  tag-text-arg = brace-enclosed
  escaped-char = escape-char any-char
  "))



(def lexer
  "The proper lexer, a version of [[lexer*]] where each rule has its tag hidden."
  (gu/hide-tags-all lexer*))


(def hidden-tags
  "Names of additional grammatical rules that have their tag hidden."
  (into #{:embedded
          :verbatim
          :escaped-char
          :tag-args}
        (keys enclosed-g)))


(def hidden-results
  "Names of rules whose productions are completely hidden."
  #{:special-char
    :pipe-char
    :tag-spaces
    :escape-char})


(def all-grammatical-rules
  "Merging of the lexer rules and the grammatical rules."
  (merge
    lexer
    enclosed-g
    general-g))


(def grammar
  "The proper grammar in map form.

  It is an updated [[all-grammatical-rules]] in regards to [[hidden-tags]] [[hidden-results]]."
  (-> all-grammatical-rules
      (gu/hide-tags hidden-tags)
      (gu/hide hidden-results)))


(def parser
  "Instaparse parser made from [[grammar]]"
  (insta/parser grammar
                :start :doc
                :output-format :enlive))

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
  (parser ex1)

  (def ex2
    "Some text
    ◊div
    [:class \"aside\"]
    {
      some other text
    }

    ◊a-tag   []
    {toto}

    ◊|@l

    ◊autoc-losed-tag   \\[]")

  (println ex2)
  (parser ex2)
  (parser "◊pollen◊\".\"")
  (parser "◊div{wanted to use the ◊\"}\" char}"))

