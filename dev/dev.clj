(ns dev
  (:require
    [cljs.repl.node :as node]
    [cider.piggieback :as piggie]))

(defn start-node-repl []
  (piggie/cljs-repl (node/repl-env)))


(comment
  (start-node-repl))

