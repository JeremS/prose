(ns fr.jeremyschoffen.prose.alpha.out.markdown.compiler
  (:require
    [fr.jeremyschoffen.prose.alpha.compilation.core :as common :refer [emit! emit-seq!]]
    [fr.jeremyschoffen.prose.alpha.out.html.compiler :as html-cplr]))


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


(defmethod common/emit-tag! [::md :md-block] [node]
  (let [{:keys [attrs content]} node
        type (get attrs :type "text")]
    (emit-block! type content)))


;;----------------------------------------------------------------------------------------------------------------------
;; Special cases
;;----------------------------------------------------------------------------------------------------------------------
(defmethod common/emit-special! [::md :comment] [_])


;;----------------------------------------------------------------------------------------------------------------------
;; Compiler
;;----------------------------------------------------------------------------------------------------------------------
(derive ::md ::html-cplr/html)
(def implementation (assoc html-cplr/implementation :name ::md))


(defn compile! [doc]
  (common/text-environment
    (common/with-implementation implementation
                                (common/emit-doc! doc))))


(comment
  (require '[fr.jeremyschoffen.prose.alpha.out.html.tags :as tags])
  (compile! ["toto" "titi"
             (tags/html5-dtd)
             (tags/comment "<some--> " "comment")
             (tags/a {:href "http://some.url.com"} "some link")])


  (println (compile! [{:tag :md-block
                       :content "(-> 1 (inc))"}
                      ">"]))
  (fr.jeremyschoffen.prose.alpha.out.html.compiler/compile! {:tag :md-block
                                                             :content "(-> 1 (inc))"}))
