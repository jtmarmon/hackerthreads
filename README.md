#hackerthreads
A demo of realtime applications using datomic and graphql.

#How to run
Run `lein repl` to start a repl in the `hackerthreads.repl` namespace. Then run
`(start-figwheel!)` and visit `http://localhost:3449` in your browser.

To add posts, run `(d/transact conn [{ :post/title "My post" :post/body "Hello world!" }])`.
You should see the result automatically propogate to your browser.

#Rationale
TODO
