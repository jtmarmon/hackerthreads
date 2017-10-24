# hackerthreads
Live queries done right. [Video demo](https://v.usetapes.com/85knuAPruB)

# Rationale
Most realtime applications are done the following way: 

`user A does something -> some logic decides who needs to be updated of what information -> update is sent`

So in the case of a chat app:

`user A sends a message -> get all users in the conversation -> send message update out to those users`

The problem with this is that any and every time you want realtime functionality for some part of your app, you have to define that logic in the middle and tell your clients to listen for the event. Over time this means hundreds of little events for every little event, and many times the only point of the event is so the client can update stale data.

Live queries are a way around that: the client sends a query up to the server and keeps a connection open. The server receives updates on the query _from the data store_ and projects those updates to the client. No events necessary<sup>1</sup>.

This is obviously complicated in a lot of traditional database systems. [Datomic,](http://www.datomic.com/) however, has a unique architecture that allows live queries to be performed due to the realtime writes being broadcasted to any connected peers. This application is a proof of concept of that.

<sup>1</sup> Events are still useful for many things. Particularly to represent actual user-relevant events, rather than data changes. They just aren't necessary for every time you want realtime data.

# Prior art
I first heard about this idea in a talk on the future of GraphQL by Lee Byron. I don't believe it's been implemented in GraphQL yet for the main reason that most people don't use a data store that allows them to do this.

# How to run
Run `lein repl` to start a repl in the `hackerthreads.repl` namespace. Then run
`(start-figwheel!)` and visit `http://localhost:3449` in your browser.

To add posts, run `(d/transact conn [{ :post/title "My post" :post/body "Hello world!" }])`.
You should see the result automatically propogate to your browser.



