


# Compiling documents
Prose provides APIs to compile data to text. The general idea being that the
result of reading then evaluating a document is data that can be compiled. The
shape of this data is modeled after the enlive or core.xml format. The
compilation APIs deal with trees of maps. These maps come usually with the key:
- `:type`: the type of the tag, for instance `:tag` or `:dtd`

The rest of the keys depends on the type of the map. In the case of the type
`:tag`:
- `:tag`: the name of a tag, for instance `:p` or `:h1`
- `:attrs`: a map of attributes
- `:content`: children of the tag, a sequence of strings or maps. Sequences are
  'spliced' in


The namespace `fr.jeremyschoffen.prose.alpha.compilation.core` provides a set of generic functions that
make our compilation algorithm. There are several goals here:
- being able to derive compilers from one another, for instance the Markdown
  compiler is based on the HTML one
- being able to specify default behaviors and openly extend special cases where
  needed

## The basic `emit!` mechanism
At the base level, documents are compiled by emitting text to an output stored
in the dynamic variable `fr.jeremyschoffen.prose.alpha.compilation.core/*compilation-out*`. Outputs are
implementations of the `Output` protocol.
```clojure
(defprotocol Output
  (append! [this text]))

```


We have Clojure and ClojureScript implementations of this protocol using
a `StringBuilder` in java and a `StringBuffer` in javascript:
```clojure
(macro/case
  :clj (defn text-output
         "Create a text output intended to be a possible binding for [[*compilation-out*]] using
         a java `java.lang.StringBuilder`."
         []
         (let [builder (StringBuilder.)]
           (reify
             Object
             (toString [_]
               (str builder))
             Output
             (append! [_ text]
               (.append builder text)))))

  :cljs (defn text-output
          "Create a text output intended to be a possible binding for [[*compilation-out*]] using
         a `goog.string StringBuffer`."
          []
          (let [builder (StringBuffer.)]
            (specify! builder
              Output
              (append! [_ text]
                (.append builder text))))))

```


The `fr.jeremyschoffen.prose.alpha.compilation.core/emit!` function is the low level primitive of our
compiler:
```clojure
(defn emit!
  "Emit text to [[*compilation-out*]].

  `args` are emitted in sequence, nil values are discarded."
  [& args]
  (doseq [text args]
    (when text
      (append! *compilation-out* text))))

```

There is a helper macro to emit to a text buffer:
```clojure
  (defmacro text-environment
    "Binds [[*compilation-out*]] to a stringbuilder using [[text-output]]"
    [& body]
    `(bind-output (text-output)
                  ~@body
                  (str *compilation-out*)))

```

We can then emit some text:
```clojure

  (compilation/text-environment
    (compilation/emit! "hello")
    (compilation/emit! " " "world")
    (compilation/emit! "!"))

```
;=>
```clojure
hello world!
```

## Generic compilation functions
Our basic emit machinery is used to create a generic compiler. The entry point
of our document compiler is the `fr.jeremyschoffen.prose.alpha.compilation.core/emit-doc!` function.
```clojure
(defn emit-doc!
  "Emit a document to [[*compilation-out*]].
  The [[*implementation*]] also needs to be bound."
  [node]
  (cond
    (lib/special? node) (emit-special! node)
    (lib/tag? node) (emit-tag! node)
    (sequential? node) (emit-seq! node)
    :else (emit-str! node)))

```

We distinguish 4 cases for which we dispatch recursively to less generic
emit functions.

- `fr.jeremyschoffen.prose.alpha.compilation.core/emit-seq!` is pretty straightforward:
```clojure
(defn emit-seq! [ss]
  (doseq [s ss]
    (emit-doc! s)))

```

- `fr.jeremyschoffen.prose.alpha.compilation.core/emit-str!` emits text using the current implementation:
```clojure
(defn emit-str!
  "Generic emit! function using the specific implementation from [[*implementation*]]."
  [s]
  ((:default-emit-str! *implementation*) s))

```

- `fr.jeremyschoffen.prose.alpha.compilation.core/emit-tag!` emits maps of type `:tag`:
```clojure
(defmulti emit-tag!
          "Generic emit-tag! function using the specific implementation from [[*implementation*]]
          by default.

          This function dispatches on a pair of value constructed like this:
          `[(:name *implementation*) (:tag node)]`, `node` being a map, the only argument of the function."
          (fn [node] [(:name *implementation*) (:tag node)]))

```
```clojure

(defmethod emit-tag! :default [s] ((:default-emit-tag! *implementation*) s))

```

- `fr.jeremyschoffen.prose.alpha.compilation.core/emit-special!` emits maps of other types:
```clojure
(defmulti emit-special!
          "Generic emit-special! function using the specific implementation from [[*implementation*]]
          by default.

          This function dispatches on a pair of value constructed like this:
          `[(:name *implementation*) (:type node)]`, `node` being a map, the only argument of the function."
          (fn [node] [(:name *implementation*) (:type node)]))

```
```clojure

(defmethod emit-special! :default [s] ((:default-emit-special! *implementation*) s))

```

In these generic functions the dynamic var `*implementation*` is used:
```clojure
(def ^:dynamic *implementation*
  "Map containing the default functions of a compiler implementation.

  It has 4 keys:
  - `:name`: the name of the implementation (a keyword)
  - `:default-emit-str!`: function that compiles plain text. The escaping logic is intended to live here.
  - `:default-emit-tag!`: function that compiles a regular tag.
  - `:default-emit-special!`: function that compiles a special tag

  By default this var provides functions that throw exceptions forcing specific implementations to
  define them."
  {:name ::default
   :default-emit-str! (fn [& args]
                        (throw (ex-info "No `:default-emit-str!` provided"
                                        {`*implementation* *implementation*
                                         :args args})))
   :default-emit-tag! (fn [& args]
                        (throw (ex-info "No `:default-emit-tag!` provided"
                                        {`*implementation* *implementation*
                                         :args args})))
   :default-emit-special! (fn [& args]
                            (throw (ex-info "No `:default-emit-special!` provided"
                                            {`*implementation* *implementation*
                                             :args args})))})

```

It allows us to declare a names for implementations and default behaviors when it comes
to emits strings, regular and classic tags.

The implementation name is used in the dispatch schemes of the
`fr.jeremyschoffen.prose.alpha.compilation.core/emit-tag!` and `fr.jeremyschoffen.prose.alpha.compilation.core/emit-special!` multimethods.
This allows us to have different compilation results of tags of the same name depending
on the implementation.


## Usage
Prose provides several compilation targets using the generic compiler.
Exploring the HTML and Markdown targets may help in understanding this design
that uses an 'implementation map' and multimethods.

### The HTML target
At its low level the HTML compiler in `fr.jeremyschoffen.prose.alpha.out.html.compiler` is using code mostly
inspired from the enlive library. There are specific functions for emitting
different types of data:
- escaped text: `fr.jeremyschoffen.prose.alpha.out.html.compiler/xml-str`
- HTML attributes: `fr.jeremyschoffen.prose.alpha.out.html.compiler/attr-str`
- regular tags: `fr.jeremyschoffen.prose.alpha.out.html.compiler/emit-tag!`
- HTML comments: `fr.jeremyschoffen.prose.alpha.out.html.compiler/emit-comment!`
- ...

At a higher level these functions are wired in the generic mechanism.
For instance:
```clojure

(defmethod common/emit-special! [::html :comment] [x]
  (emit-comment! x))

```

There is the implementation for HTML:
```clojure
(defn emit-str!
  "Default emit-str for the HTML compiler. Uses [[xml-str]] to escaped characters."
  [x]
  (emit! (xml-str x)))

```
```clojure
(def implementation
  "Html implementation of our generic compiler, this is meant to a binding to
  [[fr.jeremyschoffen.prose.alpha.compilation.core]]."
  (assoc common/*implementation*
    :name ::html
    :default-emit-str! emit-str!
    :default-emit-tag! emit-tag!))

```

With this implementation:
- strings are escaped when emitted using `fr.jeremyschoffen.prose.alpha.out.html.compiler/emit-str!`
- by default tags are compiled with `fr.jeremyschoffen.prose.alpha.out.html.compiler/emit-tag!`

Finally we define a compilation function:
```clojure
(defn compile!
  "Compile a document (in data form) into html text."
  [doc]
  (common/text-environment
    (common/with-implementation implementation
      (common/emit-doc! doc))))

```


### The markdown target
Having markdown as a compilation target proves convenient when it comes to
writing readmes or even documentation for projects. The way the format works
is by providing alternative syntaxes for several HTML tags and using regular
HTML syntax for the rest.

Capitalizing on the closeness of the 2 formats our Markdown compiler that lives
in the `fr.jeremyschoffen.prose.alpha.out.markdown.compiler` namespace just derives the HTML one and tweak it a bit.

Here is the code to override the compilation behavior for links:
```clojure

(defmethod common/emit-tag!  [::md :a] [{:keys [attrs content]}]
  (let [href (get attrs :href)]
    (emit! \[)
    (if (seq content)
      (emit-seq! content)
      (emit! href))
    (emit! \])
    (emit! \( href \))))

```

The fact that the generic `fr.jeremyschoffen.prose.alpha.compilation.core/emit-tag!` multimethod dispatches
on `[(:name fr...compilation.core/*implementation*) :a]` starts to
make sense. Instead, if the dispatch was just on the tag name, loading this
namespace would alter the compilation of links for all compilation targets.

We have a compilation scheme for a custom tag:
```clojure

(defmethod common/emit-tag! [::md ::tags/code-block] [node]
  (let [{:keys [attrs content]} node
        type (get attrs :content-type "text")]
    (emit-block! type content)))

```

The behavior for the `:comment` special tag is changed, we emit nothing since
there aren't comment in Markdown:
```clojure

(defmethod common/emit-special! [::md :comment] [_])

```

The name of this implementation derive from the name of the HTML compiler:
```clojure

(derive ::md ::html-cplr/html)

```

This way, any call to `fr.jeremyschoffen.prose.alpha.compilation.core/emit-tag!` that would
dispatch on `[::md :custom-tag]` can more generaly be caught by
`[::html-cplr/html :custom-tag]`. In other words, any custom tag with a custom
compilation will be re-used with this implementation.

The implementation map looks like this:
```clojure
(def implementation
  "Markdown implementation of our generic compiler, this is meant to a binding to
  [[fr.jeremyschoffen.prose.alpha.compilation.core]] and is based / derived from
  [[fr.jeremyschoffen.prose.alpha.out.html.compiler/implementation]]."
  (assoc html-cplr/implementation
    :name ::md
    :default-emit-str! common/emit!
    :default-emit-tag! emit-tag!))

```

The default compilation functions for strings and regular tags are set up in a
way to deal with escaping special characters:
- in Markdown proper no need to escape strings and so we use
  `fr.jeremyschoffen.prose.alpha.compilation.core/emit!`
- general html tags however won't go through escaping though so we re-instate
  escaping for them using `fr.jeremyschoffen.prose.alpha.out.markdown.compiler/emit-tag!`
```clojure
(defn emit-tag! [t]
  (common/with-implementation (assoc common/*implementation*
                                     :default-emit-str!
                                     html-cplr/emit-str!)
    (html-cplr/emit-tag! t)))

```

Finally there is a compile to text function:
```clojure
(defn compile! [doc]
  (common/text-environment
    (common/with-implementation implementation
      (common/emit-doc! doc))))

```
