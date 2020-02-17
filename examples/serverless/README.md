## Serverless example

Jadak running as a web library on [AWS Lambda](https://aws.amazon.com/lambda).

Here, Jadak is serving a simple todo-app from a Lambda*, behind an API Gateway.

The API Gateway has one "catch-all" route, so all requests are handled by Jadak.

The nice thing is however that the same code can easily be run locally 
(via the `run-local.sh` script).

Uses [aws-lumo-cljs-runtime](http://github.com/grav/aws-lumo-cljs-runtime)
for executing the ClojureScript code on AWS Lambda.

*) Since Lambdas themselves don't persist state, the items on the 
   todo-list will eventually disappear when the Lambda isn't reused anymore!  