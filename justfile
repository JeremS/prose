
nrepl_middleware := "[cider.piggieback/wrap-cljs-repl vlaaad.reveal.nrepl/middleware]"

repl:
	clojure -A:clj:cljs:dev:nrepl:piggie:reveal:test -m nrepl.cmdline --middleware "{{nrepl_middleware}}"


clj-test opts="":
	clojure -A:clj:cljs:test -m kaocha.runner unit-cljs {{opts}}


cljs-test opts="":
	clojure -A:clj:cljs:test -m kaocha.runner unit-cljs {{opts}}


toto opts:
	echo "toto {{opts}}"