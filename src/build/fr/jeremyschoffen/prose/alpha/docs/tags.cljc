(ns fr.jeremyschoffen.prose.alpha.docs.tags
  (:require
    [meander.epsilon :as m]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as md]
    [fr.jeremyschoffen.prose.alpha.out.html.compiler :as cplr]
    [fr.jeremyschoffen.prose.alpha.reader.core :as reader]))

(u/pseudo-nss project)

(def pollen (lib/xml-tag :a {:href ""} "Pollen"))

(defn make-link [href text]
  (lib/xml-tag :a {:href href} text))


(def racket (make-link "https://racket-lang.org/" "Racket"))
(def pollen (make-link "https://github.com/mbutterick/pollen" "Pollen"))
(def scribble (make-link "https://docs.racket-lang.org/scribble/index.html" "Scribble"))
(def sci (make-link "https://github.com/borkdude/sci" "Sci"))

(defn mvn-version [v]
  (lib/<>
    (md/code-block {:type "clojure"}
                   (pr-str v))))


(defn project-coords []
  (let [coords  (:project/coords (lib/get-input))
        {mvn :maven
         git :git} coords
        lein (m/find mvn
                     {?n {:mvn/version ?v}}
                     [?n ?v])]
    ["Deps coords:"
     (md/code-block {:type "clojure"}
       (binding [*print-namespace-maps* false]
         (pr-str mvn)))
     "\n"


     "Lein coords:"
     (md/code-block {:type "clojure"}
       (pr-str lein))
     "\n"

     "Git coords:"
     (md/code-block {:type "clojure"}
       (binding [*print-namespace-maps* false]
         (pr-str git)))
     "\n"]))



(defn reader-sample [path]
  (let [slurp-doc (:fr.jeremyschoffen.prose.alpha.docs.evaluation/slurp-doc (lib/get-input))
        text (slurp-doc path)]
    (lib/<>
      (md/code-block
        text)

      "\n"
      "Reads as:"
      "\n"
      (md/code-block {:type "clojure"}
                     (pr-str (reader/read-from-string text))))))


(defn eval-sample [path]
  (let [load-doc (lib/get-load-doc)
        eval-forms (lib/get-eval-doc)
        evaled (-> path
                   load-doc
                   eval-forms)
        text (with-out-str
               (clojure.pprint/pprint evaled))]

    (lib/<>
      (md/code-block
        text))))

(defn doc-sample [path]
  (let [load-doc (lib/get-load-doc)
        eval-forms (lib/get-eval-doc)
        text (-> path
                 load-doc
                 eval-forms
                 cplr/compile!)]

    (lib/<>
      (md/code-block {:type "html"}
        text))))




