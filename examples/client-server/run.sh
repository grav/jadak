#!/usr/bin/env bash

set -e

npm i && npx shadow-cljs watch client server
