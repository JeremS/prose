(ns fr.jeremyschoffen.prose.alpha.lib.compilation
  (:require
    [net.cgrand.macrovich :as macro :include-macros true])
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
