(ns docs.alpha.core
  (:require
    [clojure.string :as string]
    [fr.jeremyschoffen.java.nio.alpha.file :as fs]
    [docs.alpha.evaluation :as ev]))


(defn generate-readme! [input-map]
  (spit "README.md" (ev/document "README.md.prose" input-map)))


(defn strip-ext [file-name]
  (string/replace-first file-name ".prose" ""))


(defn make-dest [path]
  (->> path
       strip-ext
       fs/path
       fs/file-name
       (fs/path "doc")))

(defn compile-doc!
  [path]
  (spit (make-dest path) (ev/document path)))


(def docs
  ["prose/alpha/reader.md.prose"
   "prose/alpha/evaluation.md.prose"
   "prose/alpha/compilation.md.prose"])

(defn generate-design-docs! []
  (doseq [d docs]
    (compile-doc! d)))


(comment
  (-> *e ex-cause ex-data)
  (generate-design-docs!)
  (ev/document "prose/alpha/compilation.md.prose")

  (ev/document "README.md.prose" {:git-coord {}}))

