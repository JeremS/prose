(ns fr.jeremyschoffen.prose.alpha.docs.core
  (:require
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

(comment
  (ev/document "README.md.prose"
               {:project/coords {}}))
