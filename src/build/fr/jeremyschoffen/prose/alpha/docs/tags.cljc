(ns fr.jeremyschoffen.prose.alpha.docs.tags
  (:require
    [meander.epsilon :as m]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as md]
    [fr.jeremyschoffen.prose.alpha.out.html.compiler :as cplr]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))

(u/pseudo-nss project)

(defn make-link [href text]
  (lib/xml-tag :a {:href href} text))


(def racket (make-link "https://racket-lang.org/" "Racket"))
(def pollen (make-link "https://github.com/mbutterick/pollen" "Pollen"))
(def scribble (make-link "https://docs.racket-lang.org/scribble/index.html" "Scribble"))
(def sci (make-link "https://github.com/borkdude/sci" "Sci"))


(defn project-coords []
  (let [coords  (:project/coords (lib/get-input))
        {mvn :maven
         git :git} coords
        lein (m/find mvn
                     {?n {:mvn/version ?v}}
                     [?n ?v])]
    ["Deps coords:\n"
     (md/code-block {:content-type "clojure"}
       (binding [*print-namespace-maps* false]
         (pr-str mvn)))
     "\n"


     "Lein coords:\n"
     (md/code-block {:content-type "clojure"}
       (pr-str lein))
     "\n"

     "Git coords:\n"
     (md/code-block {:content-type "clojure"}
       (binding [*print-namespace-maps* false]
         (pr-str git)))
     "\n"]))


(defn reader-sample [path]
  (let [slurp-doc  (lib/get-slurp-doc)
        text (slurp-doc path)]
    (lib/<>
      (md/code-block
        text)
      "\n"
      "Reads as:"
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
