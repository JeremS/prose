(ns ^{:author "Jeremy Schoffen"
      :doc "
Api providing generic compilation utilities.
"}
  fr.jeremyschoffen.prose.alpha.compilation.core
  (:require
    [net.cgrand.macrovich :as macro :include-macros true]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib])
  #?(:cljs (:import [goog.string StringBuffer])))


;;----------------------------------------------------------------------------------------------------------------------
;; Basic compilation emit! mechanism
;;----------------------------------------------------------------------------------------------------------------------
(defprotocol Output
  (append! [this text]))


(def ^:dynamic *compilation-out*
  "Dynamic var that must be bound to an implementation of the [[Output]] protocol. It is used as the output
  of the [[emit!]] function."
  (reify Output
    (append! [_ _]
      (throw (ex-info "No compilation output provided." {})))))


(defn emit!
  "Emit text to [[*compilation-out*]].

  `args` are emitted in sequence, nil values are discarded."
  [& args]
  (doseq [text args]
    (when text
      (append! *compilation-out* text))))


(macro/deftime
  (defmacro bind-output
    "Bind [[*compilation-out*]] to `out` and execute `body` in this environment."
    [out & body]
    `(binding [*compilation-out* ~out]
       ~@body)))


;;----------------------------------------------------------------------------------------------------------------------
;; Stringbuffer implementation
;;----------------------------------------------------------------------------------------------------------------------
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

  :cljs (defn text-output []
          "Create a text output intended to be a possible binding for [[*compilation-out*]] using
         a `goog.string StringBuffer`."
          (let [builder (StringBuffer.)]
            (specify! builder
              Output
              (append! [_ text]
                (.append builder text))))))


(macro/deftime
  (defmacro text-environment
    "Binds [[*compilation-out*]] to a stringbuilder using [[text-output]]"
    [& body]
    `(bind-output (text-output)
                  ~@body
                  (str *compilation-out*))))


;;----------------------------------------------------------------------------------------------------------------------
;; Generic compiler
;;----------------------------------------------------------------------------------------------------------------------
(declare emit-doc!)


(defn emit-seq! [ss]
  (doseq [s ss]
    (emit-doc! s)))


(def ^:dynamic *implementation*
  "Map containing the default functions of a compiler implementation.

  It has 3 keys:
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


(macro/deftime
  (defmacro with-implementation
    "Binds the dynamic var [[*implementation*]] to `i`.

    See [[*implementation*]] for specifics."
    [i & body]
    `(binding [*implementation* ~i]
       ~@body)))


(defn emit-str!
  "Generic emit! function using the specific implementation from [[*implementation*]]."
  [s]
  ((:default-emit-str! *implementation*) s))


(defmulti emit-tag!
          "Generic emit-tag! function using the specific implementation from [[*implementation*]]
          by default.

          This function dispatches on a pair of value constructed like this:
          `[(:name *implementation*) (:tag node)]`, `node` being a map, the only argument of the function."
          (fn [node] [(:name *implementation*) (:tag node)]))


(defmethod emit-tag! :default [s] ((:default-emit-tag! *implementation*) s))


(defmethod emit-tag! [::default :<>] [x]
  (emit-seq! (:content x)))


(defmulti emit-special!
          "Generic emit-special! function using the specific implementation from [[*implementation*]]
          by default.

          This function dispatches on a pair of value constructed like this:
          `[(:name *implementation*) (:type node)]`, `node` being a map, the only argument of the function."
          (fn [node] [(:name *implementation*) (:type node)]))


(defmethod emit-special! :default [s] ((:default-emit-special! *implementation*) s))


(defn emit-doc!
  "Emit a document to [[*compilation-out*]].
  The [[*implementation*]] also needs to be bound."
  [node]
  (cond
    (lib/special? node) (emit-special! node)
    (lib/tag? node) (emit-tag! node)
    (sequential? node) (emit-seq! node)
    :else (emit-str! node)))
