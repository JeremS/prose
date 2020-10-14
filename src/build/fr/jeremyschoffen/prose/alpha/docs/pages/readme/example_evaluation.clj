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