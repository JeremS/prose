(ns fr.jeremyschoffen.prose.alpha.docs.core
  (:require
    [clojure.string :as string]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.prose.alpha.docs.evaluation :as ev]))

(u/pseudo-nss project)


(defn make-readme! [{wd ::project/working-dir
                     mvn-coords ::project/maven-coords
                     git-coords ::project/git-coords
                     :as conf}]
  (spit (u/safer-path wd "README.md")
        (ev/document "README.md.prose"
                     {:project/coords {:maven mvn-coords
                                       :git git-coords}})))


(defn strip-ext [file-name]
  (string/replace-first file-name ".prose" ""))


(defn compile-doc!
  [path]
  (let [dest (u/safer-path "doc" (strip-ext path))]
    (spit dest (ev/document path))))

(def docs
  ["reader.md.prose"
   "evaluation.md.prose"
   "compilation.md.prose"])

(defn make-design-docs! []
  (doseq [d docs]
    (compile-doc! d)))

(comment
  (make-design-docs!)
  (compile-doc! "README.md.prose")
  (ev/document "reader.md.prose")
  (ev/document "README.md.prose"
               {:project/coords {}}))

