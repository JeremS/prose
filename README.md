
# Prose

Alternate syntax for Clojure, similar to what [Pollen](https://github.com/mbutterick/pollen) brings to [Racket](https://racket-lang.org/).

## Installation
Git coords:
```clojure
{fr.jeremyschoffen/prose-alpha {:git/url "https://github.com/JeremS/prose.git", :sha "cbb56f6198deef76ba4298ea264992251b204421"}}
```


## Usage
The main idea is to provide a tool that allows the creation of textual documents in which clojure can be embedded.
The way Prose goes about it is by flipping the relationship between palin text and code.
In a clojure file, text is assumed to be code except in special case like strings and comments.
In prose, text is assumed to be just plain text except in special cases i.e. clojure code.

One of the main use of Prose is to use it as a text processor when wanting to generate different kinds of text
documents. The examples following will describe Prose from this angle.


### Syntax
Prose provides a reader similar to what we can find in [Pollen](https://github.com/mbutterick/pollen). Text is either plain text or a special construct.
All special constructs begin with the character `◊`(lozenge).

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


### Prose as a preprocessor
Prose provides some helpers in defining html tag. These functions work as follow:
```clojure
(div "content")
;=> {:tag :div, :content ["content"], :type :tag}

(div {} "content")
;=> {:tag :div, :attrs {}, :content ["content"], :type :tag}

(div (div) (div) (div))
;=> {:tag :div, :content [{:tag :div, :type :tag} {:tag :div, :type :tag} {:tag :div, :type :tag}], :type :tag}
```

With this api we can create documents such as:
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

This document would result in the following html:
```html
<div>
  some text
  <ul>
    <li>1</li>
    <li>2</li>
  </ul>
</div>
```

To do so we use Prose's apis this way:
```clojure
(ns fr.jeremyschoffen.prose.alpha.docs.pages.readme.example-evaluation
  (:require
    [fr.jeremyschoffen.prose.alpha.document.sci :as doc]
    [fr.jeremyschoffen.prose.alpha.document.sci.bindings :as bindings]
    [fr.jeremyschoffen.prose.alpha.eval.sci :as eval-sci]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]
    [fr.jeremyschoffen.prose.alpha.out.markdown.compiler :as cplr]


    fr.jeremyschoffen.prose.alpha.out.html.tags))


(def docs-root "src/build/fr/jeremyschoffen/prose/alpha/docs/pages/readme/")
(def example-src (str docs-root "example-doc.html.prose"))
(def example-dest (str docs-root "example-doc.html"))


;; Preparing the namespaces accessible to the sci evaluation env
(def sci-nss {:namespaces
              (bindings/make-ns-bindings
                fr.jeremyschoffen.prose.alpha.out.html.tags)})


;; Making the sci environment
(def sci-ctxt (doc/init sci-nss))


;; Making a sci eval function using our environment
(def eval-forms (partial eval-sci/eval-forms-in-temp-ns sci-ctxt))


;; Putting together a function that reads and evals documents
(def eval-doc (doc/make-evaluator {:slurp-doc slurp
                                   :read-doc reader/read-from-string
                                   :eval-forms eval-forms}))

;; Generation of the html example
(defn make-example []
  (-> example-src
      eval-doc
      cplr/compile!
      clojure.string/trim
      (->> (spit example-dest))))


(comment
  (make-example))
```


## The ◊ (lozenge) character
The first question that came to mind when I discovered [Pollen](https://github.com/mbutterick/pollen) was: why this `◊` character? I expect
the same question will arise for this project.

[Pollen](https://github.com/mbutterick/pollen) and Prose use `◊` for several reasons. Mainly this character isn't used as a special character in
programming languages. To stick to clojure, characters like `@`, `#` or even `&` have special meaning.
`◊` not being used either in clojure nor very much in plain text allows us to have expressions such as:
```text
◊(defn template [v]
   ◊div { Some value: ◊|v})
```

In this example there is prose syntax used inside clojure code without ambiguity. Using the `@` as
[Scribble](https://docs.racket-lang.org/scribble/index.html) does would cause problems:
```text
@(defn template [v]
   @div { Some value: @|v})
```

In that case which `@` hold Prose's meaning and which are a `deref` reader macro? Using `◊` gets us out of most of
these problems. When we want to use `◊` as text we can use the escaping/verbatim syntax `◊"◊"`. Also this:
```text
◊(str "◊")
```
behaves as you'd expect, the ◊ insisde the clojure string isn't special. That should be the extent of our
troubles with this character.

For reference here is the answer in the case of pollen from
[its documentation](https://docs.racket-lang.org/pollen/pollen-command-syntax.html#%28part._the-lozenge%29).


## Clojure vs sci evaluation
Prose provides 2 apis to evaluate code. The first one uses Clojure's eval function. The second uses [Sci](https://github.com/borkdude/sci).

There are pros and cons to each approach.

### Clojure
Pros:
- An evaluation can use anything that is in the classpath making requiring namespaces easier.
- The api may generally a bit easier to use.

Cons:
- An evaluation can use anything that is in the classpath which isn't secure.
- I believe clojure doesn't allow code ran outside of its main thread to create / destroy namespaces.
- Porting that functionality to Clojurescript requires going self hosted (eval needs to be there somehow).

### Sci
Pros:
- Runs in clojure and clojurescript
- bringing it's own reifed environment sci evaluation can easily happen in a background thread.
- allows us to sandbox what's accessible to the code / document being evaluated.

Cons:
- May be a bit of a perf hit.
- Managing the sci context makes for an api not as easy to use.

## License

Copyright © 2020 Jeremy Schoffen.

Distributed under the Eclipse Public License v 2.0.
