






# Prose's reader implementation.

The goal of prose's reader is to translate prose style text into clojure data
that can be evaluated by clojure's `eval` function. To do so we make use of the
instaparse and edamame libraries. Instaparse helps us separate text that is text
from text that is code. Edamame is used to read textual code into clojure data.

We explore here the way our reader is constructed.

## The instaparse grammar
We use instaparse to express prose's grammar and generate it's parser.
Instaparse doesn't make a distinction between lexer and parser. Still it is
usefull to separate our grammar using this usual lexer / grammar dichotomy in
the code.

### The lexer.
Our lexer is made of regular expressions constructed with the
`fr.jeremyschoffen.prose.alpha.reader.grammar.utils/def-regex` macro. It uses the Regal library under the covers.

To assemble these regexes into a lexer we use the `fr.jeremyschoffen.prose.alpha.reader.grammar.utils/make-lexer` macro.

Using instaparse and some helpers
```clojure

(require '[instaparse.core :as insta])
(require '[instaparse.combinators :as instac])
(require '[fr.jeremyschoffen.prose.alpha.reader.grammar.utils :as gu])
(require '[clojure.pprint :as pp])

```

```clojure

(defn p [s] (-> s pp/pprint with-out-str))

```

we can construct the following 2 rules lexer:
```clojure

(gu/def-regex number [:* :digit])

(gu/def-regex word [:* [:class ["a" "z"]]])

(def lexer (gu/make-lexer number word))

(p lexer))

```
;=>
```clojure
{:number {:tag :regexp, :regexp #"\d*"},
 :word {:tag :regexp, :regexp #"[a-z]*"}}

```

Note that our lexer is actually a (partial) instaparse grammar.


### The grammatical rules
Most of the grammatical rules are created using the ebnf notation:
```clojure

(def rules
  (instac/ebnf
    "
    doc = (token <':'>)*
    token = (number | word)
    "))

(p rules))

```
;=>
```clojure
{:doc
 {:tag :star,
  :parser
  {:tag :cat,
   :parsers
   ({:tag :nt, :keyword :token}
    {:tag :string, :string ":", :hide true})}},
 :token
 {:tag :alt,
  :parsers ({:tag :nt, :keyword :number} {:tag :nt, :keyword :word})}}

```


### Combining the two
Now that we have both a lexer and a grammar, we can simply merge them to make
our parser.
```clojure

(def parser
  (insta/parser (merge lexer rules)
                :start :doc))

(p (parser "abc:1:def:2:3:")))

```
;=>
```clojure
[:doc
 [:token [:word "abc"]]
 [:token [:number "1"]]
 [:token [:word "def"]]
 [:token [:number "2"]]
 [:token [:number "3"]]]

```


## The reader
Prose's parser generated with instaparse is insufficient to constitute a reader
by itself.

We can compare the results of the parser and the reader on an example.

```clojure

(require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])
(require '[fr.jeremyschoffen.prose.alpha.reader.grammar :as g])
(require '[fr.jeremyschoffen.prose.alpha.document.lib :as lib])

```

```clojure

(def example (lib/slurp-doc "reader/example.prose"))

example

```
;=>
```clojure
◊(def a 3)

Some ◊em{example} text: ◊|a◊"."

```

The parser by itself distinguishes between text and code:

```clojure

(-> example g/parser p)

```
;=>
```clojure
{:tag :doc,
 :content
 ({:tag :clojure-call, :content ("(" "def a 3" ")")}
  "\n\nSome "
  {:tag :tag,
   :content
   ({:tag :tag-name, :content ("em")}
    {:tag :tag-text-arg, :content ("{" "example" "}")})}
  " text: "
  {:tag :symbol-use, :content ("a")}
  "."
  "\n")}

```

The `fr.jeremyschoffen.prose.alpha.reader.core/clojurize` function takes the result of the parser and
re-arranges it into evaluable data:
```clojure

(-> example g/parser reader/clojurize p)

```
;=>
```clojure
[(def a 3) "\n\nSome " (em "example") " text: " a "." "\n"]

```

This reading in 2 phases is provided by the `fr.jeremyschoffen.prose.alpha.reader.core/read-from-string`
function.
