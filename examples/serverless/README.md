## Serverless example

Jadak running as a web library on [AWS Lambda](https://aws.amazon.com/lambda).

Here, Jadak is serving a simple todo-app from a Lambda*.

The Lambda can be invoked by API Gateway via a proxy route, 
with all requests are handled by Jadak.

This way, since Jadak is handling the routing, 
the same service can easily be run locally 
(via the `run-local.sh` script).

Uses [aws-lumo-cljs-runtime](http://github.com/grav/aws-lumo-cljs-runtime)
for executing the ClojureScript code on AWS Lambda.

*) Since Lambdas themselves don't persist state, the items on the 
   todo-list will eventually disappear when the Lambda isn't reused anymore!
   
TODO: 
- make `publish` script setup the API gateway