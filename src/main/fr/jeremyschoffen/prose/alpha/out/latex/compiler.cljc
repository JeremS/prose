(ns fr.jeremyschoffen.prose.alpha.out.latex.compiler
  (:require
    [fr.jeremyschoffen.prose.alpha.compilation.core :as common :refer [emit! emit-seq!]]))


(defn latex-str [x]
  (-> x str (.replace "\\" "\\\\")))


(defn- named? [x]
  (or (symbol x) (keyword? x)))


(defn name-str
  "Return a string representation of a name (tag or attribute) in html."
  [n]
  (cond
    (string? n) n
    (named? n) (name n)
    :else (throw (ex-info (str "Can't make an latex name from: " n) {}))))


(defn emit-opts! [opts]
  (doseq [o opts]
    (emit! (name-str o))))


(defn emit-tag! [{:keys [tag attrs content]}]
  (emit! "\\" (name-str tag))
  (when-let [opts (:latex-opts attrs)]
    (emit! "[")
    (emit-opts! opts)
    (emit! "]"))

  (emit! "{")
  (emit-seq! content)
  (emit! "}"))


(derive ::latex ::common/default)
(def implementation (assoc common/*implementation*
                      :name ::latex
                      :default-emit-str! #(emit! (latex-str %))
                      :default-emit-tag! emit-tag!))


(defn compile! [doc]
  (common/text-environment
    (common/with-implementation implementation
                                (common/emit-doc! doc))))
