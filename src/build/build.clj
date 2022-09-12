(ns build
  (:require
    [docs.alpha.core :as docs]
    [fr.jeremyschoffen.mbt2.core :as mbt]))


(def lib-name 'io.github.jerems/prose)


(defn latest-git-coord []
  (mbt/latest-git-coord :lib-name lib-name))


(defn generate-readme! []
  (docs/generate-readme! {:git-coord (latest-git-coord)}))


(defn generate-docs! []
  ;; generate stuff
  (generate-readme!)
  (docs/generate-design-docs!)
  (mbt/git-add-all!)
  (mbt/git-commit! :commit-msg "New release."))



(defn release! []
  (mbt/assert-clean-repo)
  (mbt/tag-release!)
  (generate-docs!))


(comment
  (generate-readme!)
  (release!))

