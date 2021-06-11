
# Prose

Alternate syntax for Clojure, similar to what [Pollen](https://github.com/mbutterick/pollen) brings to [Racket](https://racket-lang.org/).

## Installation
Deps coords:
```clojure
{fr.jeremyschoffen/prose-alpha {:mvn/version "30"}}
```
Lein coords:
```clojure
[fr.jeremyschoffen/prose-alpha "30"]
```
Git coords:
```clojure
{fr.jeremyschoffen/prose-alpha {:git/url "https://github.com/JeremS/prose.git", :sha "5e2c686cc723b474877193ab2d5d78317de320af"}}
```


## Usage
The main idea is to have programmable documents in Clojure. To do so, Prose
flips the relationship between plain text and code. In a clojure file, text is
assumed to be code except in special cases like strings and comments.
In prose, text is assumed to be just plain text except in special cases i.e.
clojure code.

### Syntax
Prose provides a reader similar to what we can find in [Pollen](https://github.com/mbutterick/pollen). Text is
either plain text or a special construct. All special constructs begin with
the character `◊`(lozenge).

#### Clojure calls:
The text:
```text
We can call ◊(str "code") in text
```
reads as:
```clojure
["We can call " (str "code") " in text"]
```

#### Clojure symbols:
The text:
```text
We can use symbols ◊|some-symbol
```
reads as:
```clojure
["We can use symbols " some-symbol]
```

#### Tag function:
The text:
```text
There is a tag function syntax looking like:
◊div[{:class "grid"}]{ some content}
◊div{ some ◊em{content}}

or even:
◊str{text}
```
reads as:
```clojure
["There is a tag function syntax looking like:\n" (div {:class "grid"} " some content") "\n" (div " some " (em "content")) "\n\nor even:\n" (str "text")]
```

- clojure code argument in brackets
- text argument in braces

#### Escaped / verbatim text:
The text:
```text
The ◊"◊" character.
```
reads as:
```clojure
["The " "◊" " character."]
```


### Documents as programs
To get programmable documents prose provides several apis that are meant to
work together. We have:
- a reader turning text into code as data
- an API help to evaluate that data using Clojure's eval capabilities
- an API to compile the result of evaluations into the final document

Let's see the whole process in action. We start by requiring the necessary
apis and setting up a little helper:
```clojure

(require '[clojure.java.io :as io])
(require '[clojure.pprint :as pp])
(require '[fr.jeremyschoffen.prose.alpha.reader.core :as reader])
(require '[fr.jeremyschoffen.prose.alpha.eval.common :as eval-common])
(require '[fr.jeremyschoffen.prose.alpha.out.html.compiler :as html-compiler])

```

```clojure

(defn display [x]
  (with-out-str
    (pp/pprint x)))

```

This is the document we are using for our example:
```text
◊(require '[fr.jeremyschoffen.prose.alpha.out.html.tags :refer [div ul li]])

◊div{
  some text
  ◊ul {
    ◊li {1}
    ◊li {2}
  }
}
```

Let's read it:
```clojure

(def document
  (-> "fr/jeremyschoffen/prose/alpha/docs/pages/readme/example-doc.html.prose"
    io/resource
    slurp
    reader/read-from-string))

(display document)

```
;=>
```clojure
[(require
  '[fr.jeremyschoffen.prose.alpha.out.html.tags :refer [div ul li]])
 "\n\n"
 (div
  "\n  some text\n  "
  (ul "\n    " (li "1") "\n    " (li "2") "\n  ")
  "\n")]

```

Eval it:
```clojure

(def evaled-document (eval-common/eval-forms-in-temp-ns document))

(display evaled-document)

```
;=>
```clojure
[nil
 "\n\n"
 {:tag :div,
  :content
  ["\n  some text\n  "
   {:tag :ul,
    :content
    ["\n    "
     {:tag :li, :content ["1"], :type :tag}
     "\n    "
     {:tag :li, :content ["2"], :type :tag}
     "\n  "],
    :type :tag}
   "\n"],
  :type :tag}]

```

Compile it to html:
```clojure

(html-compiler/compile! evaled-document)

```
;=>
```clojure


<div>
  some text
  <ul>
    <li>1</li>
    <li>2</li>
  </ul>
</div>
```

There are some helpers to make this process easier:
```clojure

(require '[fr.jeremyschoffen.prose.alpha.document.clojure :as doc])

```
```clojure

(defn slurp-doc [path]
  (-> path
    io/resource
    slurp))

(def evaluate (doc/make-evaluator {:slurp-doc slurp-doc
                                   :read-doc reader/read-from-string
                                   :eval-doc eval-common/eval-forms-in-temp-ns}))
(-> "fr/jeremyschoffen/prose/alpha/docs/pages/readme/example-doc.html.prose"
  evaluate
  html-compiler/compile!)

```
;=>
```clojure


<div>
  some text
  <ul>
    <li>1</li>
    <li>2</li>
  </ul>
</div>
```

The namespaces `fr.jeremyschoffen.prose.alpha.document.*` provide more
functionality than just composing `slurp`, `read` and `eval` functions.
The `make-evaluator` functions there sets up the possibility
for documents to import other documents, passing input data to documents...


## The ◊ (lozenge) character
One of the first question that came to mind when I discovered [Pollen](https://github.com/mbutterick/pollen) was:
why this `◊` character? I expect the same question will arise for this
project.

[Pollen](https://github.com/mbutterick/pollen) and Prose use `◊` for several reasons. Mainly this character
isn't used as a special character in programming languages. To stick to
Clojure, characters like `@`, `#` or even `&` have special meaning.
`◊` not being used either in clojure nor very much in plain text allows
us to have expressions such as:
```text
◊(defn template [v]
   ◊div { Some value: ◊|v})
```

In this example there is prose syntax used inside clojure code without
ambiguity. Using the `@` as [Scribble](https://docs.racket-lang.org/scribble/index.html) does would cause problems:
```text
@(defn template [v]
   @div { Some value: @|v})
```

In that case which `@` hold Prose's meaning and which are a `deref` reader
macro? Using `◊` gets us out of most of these problems. When we want to
use `◊` as text we can use the escaping/verbatim syntax `◊"◊"`.
Also this:
```text
◊(str "◊")
```
behaves as you'd expect, the ◊ insisde the clojure string isn't special.
That should be the extent of our troubles with this character.

For reference here is the answer in the case of pollen from
[its documentation](https://docs.racket-lang.org/pollen/pollen-command-syntax.html#%28part._the-lozenge%29).


## Clojure vs sci evaluation
Currently Prose provides 2 apis to evaluate code. The first one uses Clojure's
eval function. The second uses [Sci](https://github.com/borkdude/sci).

There are pros and cons to each approach.

### Clojure
Pros:
- An evaluation can use anything that is in the classpath making requiring
  namespaces easier.
- The api may generally be a bit easier to use.

Cons:
- An evaluation can use anything that is in the classpath which isn't secure.
- I believe clojure doesn't allow code ran outside of its main thread to
  create / destroy namespaces.
- Porting that functionality to Clojurescript requires going self hosted
  (eval needs to be there somehow).

### Sci
Pros:
- Runs in clojure and clojurescript
- Bringing it's own reifed environment, [Sci](https://github.com/borkdude/sci) evaluations can easily happen in
  several threads.
- Allows us to sandbox what's accessible to the code / document being evaluated.

Cons:
- May be a bit of a perf hit.
- Managing the sci context makes for an api not as easy to use.

### Limitations
At the moment using Clojure's shortened syntax for namespace qualified keywords
is a bit tricky to use, it requires knowledge of namespace aliases before
reading documents. The main reader function, using
[edamame](https://github.com/borkdude/edamame) under the hood accepts
options passed down to edamame allowing this shortened syntax.
(see the docstring of `fr.jeremyschoffen.prose.alpha.reader.core/read-from-string` and
[edamame's docs](https://github.com/borkdude/edamame#auto-resolve))


## Mentions
This work is of course inspired and influenced by [Pollen](https://github.com/mbutterick/pollen) and [Scribble](https://docs.racket-lang.org/scribble/index.html).
The [enlive](https://github.com/cgrand/enlive) library and ClojureScript are also
a big source of inspiration where document compilation is concerned.

## License

Copyright © 2020 Jeremy Schoffen.

Distributed under the Eclipse Public License v 2.0.
