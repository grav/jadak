#!/usr/bin/env bash

command -v lumo || ( echo >&2 "Please install lumo (npm i lumo-cljs)"; exit 1 )

npx lumo --classpath ~/.m2/repository/bidi/bidi/2.1.6/bidi-2.1.6.jar:../src:. -m serverless.core
