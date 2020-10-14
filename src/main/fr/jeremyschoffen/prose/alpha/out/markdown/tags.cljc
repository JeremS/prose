(ns ^{:author "Jeremy Schoffen"
      :doc "
Api containing constructor functions for markdown tags.
"}
  fr.jeremyschoffen.prose.alpha.out.markdown.tags
  (:require
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]))


(lib/def-xml-tag code-block
  "A tag representing a markdown code block.

  Uses the attribute `:content-type` to specific a language for the code block."
  ::code-block)
