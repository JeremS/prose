(ns dev
  (:require
    [cljs.repl.node :as node]
    [cider.piggieback :as piggie]
    [hyperfiddle.rcf :as rcf]))



(rcf/enable!)

(defn start-node-repl []
  (piggie/cljs-repl (node/repl-env)))


(comment
  (start-node-repl)
  (type 1)
  :cljs/quit)

