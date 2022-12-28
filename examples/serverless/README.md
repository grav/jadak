## Serverless example

Jadak running as a web library on [AWS Lambda](https://aws.amazon.com/lambda).

Here, Jadak is serving a simple todo-app from a Lambda*.

The Lambda can be invoked by API Gateway via a proxy route, 
with all requests handled by Jadak.

This way, since Jadak is handling the routing, 
the same service can easily be run locally 
(via the `run-local.sh` script).

Uses [aws-lumo-cljs-runtime](http://github.com/grav/aws-lumo-cljs-runtime)
for executing the ClojureScript code on AWS Lambda.

*) Since Lambdas themselves don't persist state, the items on the 
   todo-list will eventually disappear when the Lambda isn't reused anymore!

### Try it out

You can try out the example here:
https://uyqbgczps5.execute-api.eu-west-1.amazonaws.com/dev/app

### Deploy to AWS

The `publish-aws.sh` script deloys the example code,
but you'll need to:
1. create a lambda function via the AWS Lambda Web console:
   - select "Author from scratch"
   - function name: `jadak-serverless-example`
   - runtime: "Provide your own bootstrap ..."
2. create a new API Gateway:
   - choose type "REST"
   - create a new resource, configured as `proxy resource`
   - set the `ANY` method to invoke the `jadak-serverless-example` function
   - deploy the API to a new stage (eg `dev`)
   - note down the invoke-url of the stage (eg `https://abc.execute-api.eu-west-1.amazonaws.com/dev/app`)
3. run the `publish-aws.sh` script
   - the script pushes the code to AWS and sets the runtime to the
   [CLJS runtime based on Lumo](https://github.com/grav/aws-lumo-cljs-runtime).
     
The example web-app should now be available on the `/app` route of the stage.

### Running with NodeJS runtime
The example can also be compiled to JavaScript and run
with the regular NodeJS runtime for AWS Lambda.

To do this, compile with [Shadow CLJS](https://github.com/thheller/shadow-cljs):
```bash
$ npm i && npx shadow-cljs release lib
```

Then, zip the released file to make it uploadable via the AWS Console:
```bash
$ zip -j lambda-js.zip lib.js
```

Then, create a new AWS Lambda function with eg the NodeJS runtime, and
upload the zip-file.

Change the handler to `lib.main`.

The API Gateway can be configured as with the previous example.


