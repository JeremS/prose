(ns ^{:author "Jeremy Schoffen"
      :doc "
Api containing constructor functions for html tags.
"}
  fr.jeremyschoffen.prose.alpha.out.html.tags
  (:refer-clojure
    :exclude [map meta time var comment mask])
  (:require
    [fr.jeremyschoffen.prose.alpha.document.lib :as lib]))



(lib/def-xml-tags
  a abbr acronym address applet area article aside audio
  b base basefont bdi bdo big blockquote body br button
  canvas caption center cite code col colgroup
  data datalist dd del details dfn dialog div dl dt
  em embed
  fieldset figcaption figure font footer form frame frameset
  h1 h2 h3 h4 h5 h6 head header hr html
  i iframe img input ins
  kbd keygen
  label legend li link
  main map mark meta menu menuitem meter
  nav noscript
  object ol optgroup option output
  p param picture pre progress
  q
  rp rt ruby
  s samp script section select small source span strike strong style sub summary sup
  table tbody td textarea tfoot th thead time title tr track
  u ul
  var video
  wbr

  ;; svg
  circle clipPath
  ellipse
  g
  line
  mask
  path pattern polyline
  rect
  svg
  text
  defs
  linearGradient
  polygon
  radialGradient
  stop
  tspan)

(lib/def-xml-tag <> "The fragment tag.")


(defn dtd [name public-id system-id]
  {:tag :special
   :type :dtd
   :data [name public-id system-id]})


(defn html5-dtd
  "The html 5 doctype tag."
  [& _]
  (dtd "html" nil nil))


(defn comment
  "A html comment."
  [& args]
  {:tag :special :type :comment, :data (vec args)})
