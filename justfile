
nrepl_middleware := "[cider.piggieback/wrap-cljs-repl vlaaad.reveal.nrepl/middleware]"

repl:
	clojure -M:clj:cljs:dev:nrepl:piggie:reveal:test -m nrepl.cmdline --middleware "{{nrepl_middleware}}"

repl-build:
	clojure -M:clj:nrepl:reveal:build -m nrepl.cmdline --middleware "[vlaaad.reveal.nrepl/middleware]"

clj-test opts="":
	clojure -M:clj:cljs:test -m kaocha.runner unit-clj {{opts}}

cljs-test opts="":
	clojure -M:clj:cljs:test -m kaocha.runner unit-cljs {{opts}}
