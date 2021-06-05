(ns fr.jeremyschoffen.prose.alpha.build
  (:require
    [clojure.spec.test.alpha :as st]
    [fr.jeremyschoffen.mbt.alpha.core :as mbt-core]
    [fr.jeremyschoffen.mbt.alpha.default :as mbt-defaults]
    [fr.jeremyschoffen.mbt.alpha.mbt-style :as mbt-build]
    [fr.jeremyschoffen.mbt.alpha.utils :as u]
    [fr.jeremyschoffen.prose.alpha.docs.core :as docs]
    [build :refer [token]]))


(u/pseudo-nss
  git
  git.commit
  maven
  maven.credentials
  project
  project.license
  version-file
  versioning)


(def conf (mbt-defaults/config
            {::maven/group-id    'fr.jeremyschoffen
             ::project/author    "Jeremy Schoffen"
             ::project/git-url   "https://github.com/JeremS/prose.git"


             ::versioning/scheme mbt-defaults/git-distance-scheme
             ::versioning/major  :alpha

             ::maven/server mbt-defaults/clojars

             ::project/licenses  [{::project.license/name "Eclipse Public License - v 2.0"
                                   ::project.license/url "https://www.eclipse.org/legal/epl-v20.html"
                                   ::project.license/distribution :repo
                                   ::project.license/file (u/safer-path "LICENSE")}]}))


(defn generate-docs! [conf]
  (-> conf
      mbt-build/merge-last-version
      (u/assoc-computed ::project/git-coords mbt-defaults/deps-make-git-coords
                        ::project/maven-coords mbt-defaults/deps-make-maven-coords)
      (assoc-in [::git/commit! ::git.commit/message] "Generated the docs.")
      (mbt-defaults/generate-then-commit!
        (u/do-side-effect! docs/make-readme!)
        (u/do-side-effect! docs/make-design-docs!))))


(u/spec-op generate-docs!
           :deps [mbt-build/merge-last-version
                  mbt-defaults/deps-make-maven-coords
                  mbt-defaults/deps-make-git-coords]
           :param {:req [::git/repo
                         ::maven/artefact-name
                         ::maven/group-id
                         ::project/git-url
                         ::versioning/scheme
                         ::versioning/tag-base-name]
                   :opt [::maven/classifier
                         ::versioning/tag-base-name
                         ::versioning/version]})


(defn bump-project! []
  (-> conf
      (u/do-side-effect! mbt-build/bump-project!)
      (u/do-side-effect! generate-docs!)))


(st/instrument `[generate-docs!
                 mbt-defaults/generate-then-commit!
                 mbt-defaults/deps-make-maven-coords
                 mbt-defaults/deps-make-git-coords
                 mbt-build/merge-last-version
                 mbt-build/build!
                 mbt-build/install!
                 mbt-build/deploy!])

(comment
  (mbt-core/clean! conf)

  (generate-docs! conf)
  (bump-project!)

  (mbt-build/build! conf)

  #_(mbt-build/install! conf)

  (-> conf
      (assoc ::maven/credentials {::maven.credentials/user-name "jeremys"
                                  ::maven.credentials/password token})
      mbt-build/deploy!))
