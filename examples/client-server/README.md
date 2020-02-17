## Client-Server example

Simple example with Jadak running an API for a web-application.

To start compilation of client and server:
```
$ npm install && npx shadow-cljs watch client server
```

Start the server:

```
$ node target/mainjs
```

Start the client by visiting http://localhost:9876

Both the client and server reload on code changes.
