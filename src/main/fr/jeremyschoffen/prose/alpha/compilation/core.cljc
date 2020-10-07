(ns fr.jeremyschoffen.prose.alpha.compilation.core
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
  (reify Output
    (append! [_ _]
      (throw (ex-info "No compilation output provided." {})))))


(defn emit! [& args]
  (doseq [text args]
    (when text
      (append! *compilation-out* text))))


(macro/deftime
  (defmacro bind-output [out & body]
    `(binding [*compilation-out* ~out]
       ~@body)))


;;----------------------------------------------------------------------------------------------------------------------
;; Stringbuffer implementation
;;----------------------------------------------------------------------------------------------------------------------
(macro/case
  :clj (defn text-output []
         (let [builder (StringBuilder.)]
           (reify
             Object
             (toString [_]
               (str builder))
             Output
             (append! [_ text]
               (.append builder text)))))

  :cljs (defn text-output []
          (let [builder (StringBuffer.)]
            (specify! builder
              Output
              (append! [_ text]
                (.append builder text))))))


(macro/deftime
  (defmacro text-environment [& body]
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


(def ^:dynamic *implementation* {:name ::default
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
    "Binds the dynamic var [[*implementation*]] to `i`."
    [i & body]
    `(binding [*implementation* ~i]
       ~@body)))


(defn emit-str! [s]
  ((:default-emit-str! *implementation*) s))



(defmulti emit-tag! (fn [node] [(:name *implementation*) (:tag node)]))
(defmethod emit-tag! :default [s] ((:default-emit-tag! *implementation*) s))


(defmethod emit-tag! [::default :<>] [x]
  (emit-seq! (:content x)))


(defmulti emit-special! (fn [node] [(:name *implementation*) (:type node)]))
(defmethod emit-special! :default [s] ((:default-emit-special! *implementation*) s))


(defn emit-doc! [node]
  "Emit a document to [[*compilation-out*]].
  The [[*implementation*]] also needs to be bound"
  (cond
    (lib/special? node) (emit-special! node)
    (lib/tag? node) (emit-tag! node)
    (sequential? node) (emit-seq! node)
    :else (emit-str! node)))
