
nrepl_middleware := "[cider.piggieback/wrap-cljs-repl vlaaad.reveal.nrepl/middleware]"

repl:
	clojure -A:clj:cljs:dev:nrepl:piggie:reveal:test -m nrepl.cmdline --middleware "{{nrepl_middleware}}"

repl-build:
	clojure -A:clj:nrepl:reveal:build -m nrepl.cmdline --middleware "[vlaaad.reveal.nrepl/middleware]"

clj-test opts="":
	clojure -A:clj:cljs:test -m kaocha.runner unit-clj {{opts}}


cljs-test opts="":
	clojure -A:clj:cljs:test -m kaocha.runner unit-cljs {{opts}}
