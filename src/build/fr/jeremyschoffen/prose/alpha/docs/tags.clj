(ns fr.jeremyschoffen.prose.alpha.docs.tags
  (:require
    [clojure.repl]
    [clojure.string :as string]
    [meander.epsilon :as m]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as md]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))

(u/pseudo-nss project)

;; -----------------------------------------------------------------------------
;; Useful links
;; -----------------------------------------------------------------------------
(defn make-link [href text]
  (lib/xml-tag :a {:href href} text))


(def racket (make-link "https://racket-lang.org/" "Racket"))
(def pollen (make-link "https://github.com/mbutterick/pollen" "Pollen"))
(def scribble (make-link "https://docs.racket-lang.org/scribble/index.html" "Scribble"))
(def sci (make-link "https://github.com/borkdude/sci" "Sci"))


;; -----------------------------------------------------------------------------
;; Tags used in the readme to display vesion info.
;; -----------------------------------------------------------------------------
(defn project-coords []
  (let [coords  (:project/coords (lib/get-input))
        {mvn :maven
         git :git} coords
        lein (m/find mvn
                     {?n {:mvn/version ?v}}
                     [?n ?v])]
    [(when mvn
       ["Deps coords:\n"
        (md/code-block {:content-type "clojure"}
                       (binding [*print-namespace-maps* false]
                         (pr-str mvn)))
        "\n"])


     (when mvn
       ["Lein coords:\n"
        (md/code-block {:content-type "clojure"}
                       (pr-str lein))
        "\n"])

     (when git
       ["Git coords:\n"
        (md/code-block {:content-type "clojure"}
                       (binding [*print-namespace-maps* false]
                         (pr-str git)))
        "\n"])]))


;; -----------------------------------------------------------------------------
;; Tags used in the readme to display examples.
;; -----------------------------------------------------------------------------
(defn reader-sample [path]
  (let [text (lib/slurp-doc path)]
    (lib/<>
      "The text:\n"
      (md/code-block
        text)
      "\n"
      "reads as:"
      "\n"
      (md/code-block {:content-type "clojure"}
                     (pr-str (reader/read-from-string text))))))

(defn make-sample-tag [t]
  (fn [path]
    (let [slurp-doc  (lib/get-slurp-doc)]
      (md/code-block {:content-type t}
                     (clojure.string/trim (slurp-doc path))))))


(def text-sample (make-sample-tag "text"))
(def html-sample (make-sample-tag "html"))
(def clojure-sample (make-sample-tag "clojure"))


;; -----------------------------------------------------------------------------
;; Code samples.
;; -----------------------------------------------------------------------------
(defn clj [& args]
  (apply md/code-block {:content-type "clojure"} args))


(defmacro code [& body]
  (let [body (string/join body)
        read-code (-> body
                    (as-> s (str "(do " s ")"))
                    (reader/read-string*))]
    (lib/<>
      (clj body)
      "\n;=>\n"
      (clj read-code))))


(defmacro code-s [& body]
  (let [body (string/join body)
        read-code (-> body
                    (as-> s (str "(do " s ")"))
                    (reader/read-string*))]
    (lib/<>
      (md/code-block {:content-type "clojure"} body)
      `(do ~read-code ""))))


(defmacro source [sym]
  `(clj
     (with-out-str
       (clojure.repl/source ~sym))))



(defn ns-qualify-ish [s]
  (if-let [n (some-> s namespace symbol)]
    (or (some-> (get (ns-aliases *ns*) n) str (symbol (name s)))
        s)
    (or (some-> (get (ns-aliases *ns*) s) str symbol)
        (symbol (-> *ns* .getName str) (name s)))))



(defmacro sym [s]
  `(str "`" ~(str (ns-qualify-ish s) "`")))

