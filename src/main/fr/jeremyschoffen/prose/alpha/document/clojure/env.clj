(ns fr.jeremyschoffen.prose.alpha.document.clojure.env)


(def ^:dynamic *load-document* (fn [& _]
                                 (throw (ex-info "Unbound load document function." {}))))

(def ^:dynamic *input* {})
