(ns fr.jeremyschoffen.prose.alpha.document.sci.bindings
  (:require
    [meander.epsilon :as m :include-macros true]))

;;----------------------------------------------------------------------------------------------------------------------
;; Sci namespace bindings helpers
;;----------------------------------------------------------------------------------------------------------------------
(defn macro?
  "True if the var `v` references a macro."
  [v]
  (some-> v
          meta
          :macro))


(defn var->binding
  "De-reference a var and add the `sci/macro` metadata if needed."
  [v]
  (-> v
      deref
      (cond-> (macro? v) (vary-meta assoc :sci/macro true))))


#_{:clj-kondo/ignore [:unresolved-symbol :unresolved-var]}
(defn publics->bindings
  "Make a sci bindings map from the result of a `ns-publics` result.

  The vars are de-referenced and in the case of macros the `sci/macro` metadata is added."
  [m]
  (m/rewrite m
             (m/map-of !name !var)
             (m/map-of !name (m/app var->binding !var))))


(defmacro bindings
  "Extract bindings by using [[publics->bindings]] on the result of `ns-publics`.

  The vars returned by `ns-publics` (the map's values) are de-referenced, in the case of macros
  the `sci/macro` metadata is added."
  [n]
  `(publics->bindings (ns-publics '~n)))


#_{:clj-kondo/ignore [:unresolved-symbol :unresolved-var]}
(defmacro make-ns-bindings
  "Make a namespaces bindings map.
  Typically used as `(sci/init {:namespaces (make-ns-bindings ns1 ns2)})`."
  [& nss]
  (m/rewrite nss
             (m/seqable (m/and !ns !ns') ...)
             (m/map-of ('quote !ns)
                       (`bindings !ns'))))


#_{:clj-kondo/ignore [:unresolved-symbol]}
(comment
  (macroexpand-1 '(make-ns-bindings fr.jeremyschoffen.prose.alpha.document.lib))
  (make-ns-bindings fr.jeremyschoffen.prose.alpha.document.lib))
