
# Jadak

Jadak is a web framework for the NodeJS platform implemented in ClojureScript.

[![Clojars Project](https://img.shields.io/clojars/v/jadak.svg)](https://clojars.org/jadak)

[![Jadak, jadak og ... jadak](https://img.youtube.com/vi/UcrMKs3xy28/0.jpg)](https://www.youtube.com/watch?v=UcrMKs3xy28&t=0m34s)

<i>Jadak, jadak og ... jadak!</i>

## Features

Jadak implements a subset of the [Yada](https://github.com/juxt/yada) web library.
Among Jadak's features are:

- declarative route definitions (via [Bidi](https://github.com/juxt/bidi))
- Basic and Cookie authentication
- CORS support
- EDN and JSON encoding and decoding
- easy usage in AWS Lambda

## Getting started

### Simple example

Here we create a single route, `/hello` that just responds `"world"`. We then use the `listener` function to 
serve the route with the built-in http-server in NodeJS:

```clojure
(require '[jadak.jadak :as jadak])

(def routes
  ["/hello" (jadak/resource {:methods {:get {:produces #{"text/plain"}
                                             :response (fn [_] "world")}}})])

(jadak/listener
  routes
  {:port 8000})
```

We can run it with [Lumo](https://github.com/anmonteiro/lumo) by saving it as `hello.cljs` and running:

```bash
$ lumo -c`clojure -Sdeps '{:deps {jadak {:mvn/version "0.1.2"}}}' -Spath` hello.cljs
```

Now we can test it using `curl`:

```bash
$ curl  http://localhost:8000/hello
world
```
