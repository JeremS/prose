(ns ^{:author "Jeremy Schoffen",
      :doc "
      # Textp's grammar.

      We construct here textp's grammar using instaparse. Our grammar is then constructed here in two parts:
      - a lexical part or lexer made of regular expressions.
      - a set of grammatical rules tyring the lexer together into the grammar.

      ## The lexer.
      Our lexer is made of regular expression constructed with the [[textp.reader.alpha.grammar/defregex]] macro
      which uses the Regal library under the covers. We then assemble a lexer from these regular expressions
      with the [[textp.reader.alpha.grammar/make-lexer]] macro.

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
      We use the [[instaparse.combinators/ebnf]] function to produce grammatical rules. This allows use
      to write these rules in the ebnf format.

      For instance we could write the following:
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

      This way of writing the grammatical rules is way easier than using function combinators and still gives us
      these rules in map form.

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

      With the exception of some details, this is how this namespace is made.
      "}
  fr.jeremyschoffen.prose.alpha.reader.grammar
  #?(:cljs (:require-macros [fr.jeremyschoffen.prose.alpha.reader.grammar :refer [def-regex make-lexer]]))
  (:require
    [clojure.set]
    [net.cgrand.macrovich :as macro :include-macros true]
    [instaparse.core :as insta]
    [instaparse.combinators :as instac]
    [medley.core :as medley]

    [lambdaisland.regal :as regal]))



;; ---------------------------------------------------------------------------------------------------------------------
;; Macro utils
;; ---------------------------------------------------------------------------------------------------------------------

(macro/deftime
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
    "Make a sequence of named regular expression into a intaparse map of named regex rules."
    [& regexes]
    `(into {}
           ~(vec (for [r regexes]
                   (let [kw (-> r name keyword)]
                     `[~kw (instac/regexp ~r)]))))))


(defmethod regal/-regal->ir [:*? :common] [[_ & rs] opts]
  (regal/quantifier->ir "*?" rs opts))

;; ---------------------------------------------------------------------------------------------------------------------
;; Lexer
;; ---------------------------------------------------------------------------------------------------------------------
(def diamond \◊)
(def escaper "\\")

;; classic delimiters
(def parens "()")
(def brackets "[]")
(def braces "{}")
(def double-quote "\"")

(def all-delimitors (sorted-set parens brackets braces double-quote))

(def non-special [:not diamond])
(def anything [:class :whitespace :non-whitespace])

(def end-verbatim       [:cat \!  diamond])
(def end-comment        [:cat \/  diamond])
(def end-embedded-value [:cat \|  diamond])
(def end-embeded-code   [:alt diamond [:cat \)  diamond]]) ;; here we also had the case of a tag inside a clojure section


(def normal-text [:not diamond escaper])


;; ---------------------------------------------------------------------------------------------------------------------
;; plain text
(def-regex plain-text
           "Text to be interpreted as plain text, neither clojure code, nor special blocks of text.
           Basically any character excluding diamond and backslash which have special meaning."
           [:* normal-text])


(def-regex escaping-char
           "The backslash used to escaped characters in plain text."
           escaper)


(def-regex any-char
           "Regex that recognizes any character."
           anything)


;; ---------------------------------------------------------------------------------------------------------------------
;; verbatim & comment text blocks
(def-regex text-verbatim
           "Regex used to parse text inside a verbatim block.
           All characters allowed terminated by \"!◊\""
           [:cat [:*? anything] [:lookahead end-verbatim]])


(def-regex text-comment
           "Regex used to parse text inside a comment block.
           All characters allowed terminated by \"/◊\""
           [:cat [:*? anything] [:lookahead end-comment]])


;; ---------------------------------------------------------------------------------------------------------------------
;; embedded

(def ns-end \/)
(def macro-reader-char \#)

(def symbol-regular-char-set
  (into #{:whitespace diamond ns-end escaper} all-delimitors))

(def symbol-regular-char
  "Characters that are always forbidden in symbol names:
  - spaces
  - diamond char since it starts another grammatical rule
  - delimitors: parens, brackets, braces  and double quotes.
  - `/` since it the special meaning of separating the namespace from the symbol name.
  - `.` since it has the special meaning of separating symbol names.
  - `\\` since it is reserved by clojure to identify a literal character."
  (into [:not ] symbol-regular-char-set))


(def symbol-first-char
  "In the case of the first character of a symbol name, there are more forbidden chars:
  - digits aren't allowed as first character
  - the macro reader char `#` isn't allowed either."
  (into [:not]
        (conj symbol-regular-char-set
              :digit
              macro-reader-char)))


(defn make-simple-symbol-regex
  "Regex for simple symbols without namespaces.
  The character repetition is parameterized to allow for reluctant repetition."
  [rep]
  [:cat symbol-first-char [rep symbol-regular-char]])


(def symbol-ns-part
  "Regex for the ns name of a symbol, parses dot separated names until
  a final name."
  (make-simple-symbol-regex :*))


(defn make-complex-symbol-regex
  "Regex for a full symbol name with namespace. Parse an optional ns name followed by
  the character `/` then a simple symbol. The repetition for the character of the symbol name
  is parameterized to allow fo reluctant repetition."
  [rep]
  (let [ns-part [:cat [:capture symbol-ns-part] ns-end]
        name-part [:capture (make-simple-symbol-regex rep)]]
    [:cat [:? ns-part] name-part]))


(def-regex text-symbol
           "Regex used when parsing a symbol in the case of tag names."
           (make-complex-symbol-regex :*))


(def-regex text-e-value
           "Regex used when parsing a symbol in the case of embedded values. It basically
           is the same as `text-symbol` except for the use of reluctant repetition for the
           symbol name and the use of a lookahead at the end to search for the end of an embedded
           value block."
           [:cat (make-complex-symbol-regex :*?)
                 [:lookahead end-embedded-value]])


(def-regex text-e-code
           "Regex used when parsing parsing text in embedded code."
           [:cat [:*? non-special]
                 [:lookahead end-embeded-code]])


;; ---------------------------------------------------------------------------------------------------------------------
;; Tags
(def text-t-clj-non-special [:not diamond brackets double-quote escaper])
(def text-escaped-char [:cat escaper anything])


(def-regex text-t-clj
           "Regex used when parsing a the text inside a clojure argument to a tag.
           Can be anything but the chars:
           - `◊`: diamond will start a new grammatical  rule
           - `[]`, brackets: theses characters will start a new grammatical rule
           - `\"`: double quote will start a new grammatical rule

           Allows for the forbidden char to appear when escaped with a backslash.
           "
           [:* [:alt text-t-clj-non-special text-escaped-char]])


(def text-t-clj-str-non-special [:not double-quote escaper])


(def-regex text-t-clj-str
           "The text inside a clojure string. Can be anything but the char:
           - `\"`: double quote will close the string

           Allows for the forbidden chars to appear when escaped with a backslash.
           "
           [:* [:alt text-t-clj-str-non-special
                         text-escaped-char]])


(def text-t-txt-non-special (conj normal-text "}"))


(def-regex tag-plain-text
           "The text found inside curly braces in tags. Can be anything but the chars:
           - `◊`: diamond will start a new grammatical rule
           - `}`: right curly brace closes the text arg to the tag
           - '\\' : backslash with start an escaped char grammatical rule

           Allows for the forbidden chars to appear when escaped with a backslash."
           [:* text-t-txt-non-special])


(def-regex text-spaces
           "Spaces found inbetween tag args."
           [:* :whitespace])


;; ---------------------------------------------------------------------------------------------------------------------
;; Grammar
;; ---------------------------------------------------------------------------------------------------------------------
(defn hide-all
  "Hide all rules in a instaparse grammar in its data (map) form by applying
  [[instaparse.combinators/hide-tag]] to all values of the map."
  [g]
  (medley/map-vals instac/hide-tag g))


(defn hide-rules
  "Selectively hide rules instaparse grammar in its data (map) form. It
  applies [[instaparse.combinators/hide-tag]] to the rules whose names are in `rule-names`."
  [g rule-names]
  (-> g
      (select-keys rule-names)
      hide-all
      (->> (merge g))))

(def lexer*
  "Raw lexer of our grammar. It's an instaparse grammar in data (map) form containing all the
  regular expressions used in the final parser."
  (make-lexer
    plain-text
    escaping-char
    any-char

    text-verbatim
    text-comment

    text-symbol
    text-e-value
    text-e-code
    text-t-clj
    text-t-clj-str
    tag-plain-text
    text-spaces))

(def lexer
  "Lexer of our grammar. Its the raw lexer with all rules are hidden by default
  (they won't materialize as a node of a parse tree)."
  (hide-all lexer*))



(def text-g
  "Grammatical rules for top level text. Basically any character except \"◊\" or any escaped character."
  (instac/ebnf
    "
    text         = plain-text | escaped-char
    escaped-char = <escaping-char> any-char
    "))


(def text-g-masked
  #{:text
    :escaped-char})


(def verbatim-g
  "Grammatical rule for verbatim text:
  ```text
  This text is normal text.
  ◊!The text here is kept ◊verbatim!◊
  ```
  "
  (instac/ebnf
    "
    verbatim = <'◊!'> text-verbatim <'!◊'>
    "))


(def comment-g
  "Grammatical rule for commented text:
  ```text
  This text is normal text.
  ◊/The text here is kept commented out/◊
  ```
  "
  (instac/ebnf
    "
    comment = <'◊/'> text-comment <'/◊'>
    "))


(def embedded-g
  "Grammatical rules descripbing clojure code embedded in text.
  ```text
  We can embed clojure calls: ◊(def ex 1)◊ and clojure values ◊|x|◊

  Not that the embedded call syntax is mutually recursive with the tag syntax.
  We can have :
  ◊(def home ◊a[:href \"www.home.com\"]{Home})◊

  and use it here: ◊|home|◊
  ```"
  (instac/ebnf
    "
    embedded       = embedded-code | embedded-value
    embedded-code  = <'◊'> '(' text-e-code (tag | text-e-code )* ')' <'◊'>
    embedded-value = <'◊|'> text-e-value                             <'|◊'>
    "))


(def embedded-g-masked
  #{:embedded})


(def tag-g
  "Grammatical rules for tag syntax.

  A tag is meant to ultimately be a clojure call.
  It starts with the character ◊ followed by a symbol then followed by arguments.
  Arguments can be clojure arguments enclosed in brackets or text argument enclosed in braces.

  Clojure arguments allow clojure code to be passed argument as embedded code which can contain other tags.
  Text argument are block of text which can recursively contain tags and embedded code."
  (instac/ebnf
    "
    tag            = <'◊'> tag-name  tag-args* !tag-args
    tag-name       = text-symbol
    tag-args       = <text-spaces> (tag-args-clj | tag-args-txt)

    tag-args-clj   = sqbrk-enclosed
    sqbrk-enclosed =  '['  (clj-txt | sqbrk-enclosed | tag)* ']'
    clj-txt        =  (text-t-clj | string)*
    string         =  '\"' text-t-clj-str '\"'

    tag-args-txt   = brk-enclosed
    brk-enclosed   = <'{'>  (tag-text | special)*         <'}'>
    tag-text       = tag-plain-text | escaped-char
    escaped-char   = <escaping-char> any-char
    "))


(def tag-g-masked
  #{:tag-args
    :tag-text
    :embedded
    :sqbrk-enclosed
    :brk-enclosed
    :clj-txt
    :string})


(def general-g
  (instac/ebnf
    "
  doc     = (text | special)*
  special = block | clojure
  block   = verbatim | comment
  clojure = embedded | tag
  "))

(def general-g-masked
  #{:special
    :block
    :clojure})

;; ---------------------------------------------------------------------------------------------------------------------
;; Assembling the parser
;; ---------------------------------------------------------------------------------------------------------------------
(def grammar-masked
  "The set of the rule names that need to be hidden. These rules won't
  produce nodes in the parse tree. In compiler parlance these are the node you'd find in a the syntax tree but not
  in the abstract syntax tree."
  (clojure.set/union text-g-masked
                     embedded-g-masked
                     tag-g-masked
                     general-g-masked))


(def all-grammatical-rules
  "Merging of the lexer rules and the grammatical rules."
  (merge
    lexer
    text-g
    verbatim-g
    comment-g
    embedded-g
    tag-g
    general-g))


(def grammar
  "Final grammar with all the rules that need to be hidden specified as such."
  (hide-rules all-grammatical-rules grammar-masked))


(def parser
  "Our parser with the starting rule specified as the `:doc` rule and the output format tree
  set to `:enlive`."
  (insta/parser grammar
                :start :doc
                :output-format :enlive))


(comment
  (def full-parser
    (insta/parser all-grammatical-rules
                  :start :doc))

  (def ex1
    "Hello my name is ◊em{Jeremy}{Schoffen}.
     We can embed code ◊(+ 1 2 3)◊.
     We can even embed tags in code:
     ◊(call ◊text{◊em{Me!}})◊

     Tags ins tags args:
     ◊toto[:arg1 ◊em{toto} :arg2 2 :arg3 \"arg 3\"].

     The craziest, we can embed ad nauseam:

     ◊(defn template [x]
        ◊div
        {
          the value x: ◊|x|◊
          the value x++: ◊(inc x)◊
        })◊")
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

    ◊|@l|◊

    ◊autoc-losed-tag   \\[]")

  (println ex2)
  (parser ex2)
  (parser "◊|poll\\en|◊.")
  (parser "◊pollen\\.")
  (parser "◊div{wanted to use the \\} char}")
  (parser "\\} \\◊"))
