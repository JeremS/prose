
nrepl_middleware := "[cider.piggieback/wrap-cljs-repl]"

repl:
	clojure -M:clj:cljs:dev:nrepl:piggie:test -m nrepl.cmdline --middleware "{{nrepl_middleware}}"

repl-build:
	clojure -M:clj:nrepl:build -m nrepl.cmdline

clj-test opts="":
	clojure -M:clj:cljs:test -m kaocha.runner unit-clj {{opts}}

cljs-test opts="":
	clojure -M:clj:cljs:test -m kaocha.runner unit-cljs {{opts}}
