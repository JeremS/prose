(ns ^{:author "Jeremy Schoffen"
      :doc "
Specialization of the generic compiler from [[fr.jeremyschoffen.prose.alpha.compilation.core]]
to compile to markdown.
"}
  fr.jeremyschoffen.prose.alpha.out.markdown.compiler
  (:require
    [fr.jeremyschoffen.prose.alpha.compilation.core :as common :refer [emit! emit-seq!]]
    [fr.jeremyschoffen.prose.alpha.out.html.compiler :as html-cplr]
    [fr.jeremyschoffen.prose.alpha.out.markdown.tags :as tags]))


;;----------------------------------------------------------------------------------------------------------------------
;; markdown links
;;----------------------------------------------------------------------------------------------------------------------
(defmethod common/emit-tag!  [::md :a] [{:keys [attrs content]}]
  (let [href (get attrs :href)]
    (emit! \[)
    (if (seq content)
      (emit-seq! content)
      (emit! href))
    (emit! \])
    (emit! \( href \))))


;;----------------------------------------------------------------------------------------------------------------------
;; markdown code blocks
;;----------------------------------------------------------------------------------------------------------------------
(defn emit-newline! [] (emit! "\n"))


(defn emit-block! [type content]
  (emit! "```" type)
  (emit-newline!)
  (doseq [c content]
    (emit! c))
  (emit-newline!)
  (emit! "```"))


(defmethod common/emit-tag! [::md ::tags/code-block] [node]
  (let [{:keys [attrs content]} node
        type (get attrs :content-type "text")]
    (emit-block! type content)))


;;----------------------------------------------------------------------------------------------------------------------
;; Special cases
;;----------------------------------------------------------------------------------------------------------------------
(defmethod common/emit-special! [::md :comment] [_])


;;----------------------------------------------------------------------------------------------------------------------
;; Compiler
;;----------------------------------------------------------------------------------------------------------------------
(derive ::md ::html-cplr/html)

(defn emit-tag! [t]
  (common/with-implementation (assoc common/*implementation*
                                     :default-emit-str!
                                     html-cplr/emit-str!)
    (html-cplr/emit-tag! t)))

(def implementation
  "Markdown implementation of our generic compiler, this is meant to a binding to
  [[fr.jeremyschoffen.prose.alpha.compilation.core]] and is based / derived from
  [[fr.jeremyschoffen.prose.alpha.out.html.compiler/implementation]]."
  (assoc html-cplr/implementation
    :name ::md
    :default-emit-str! common/emit!
    :default-emit-tag! emit-tag!))


(defn compile! [doc]
  (common/text-environment
    (common/with-implementation implementation
      (common/emit-doc! doc))))


(comment
  (require '[fr.jeremyschoffen.prose.alpha.out.html.tags :as html-tags])
  (compile! ["toto" "titi"
             (html-tags/html5-dtd)
             (html-tags/comment "<some--> " "comment")
             (html-tags/a {:href "http://some.url.com"} "some link")])


  (println (compile! [(tags/code-block {:type 'clojure} "(-> 1 (inc))")
                      "\n>"]))
  (fr.jeremyschoffen.prose.alpha.out.html.compiler/compile! {:tag :md-block
                                                             :content "(-> 1 (inc))"}))
