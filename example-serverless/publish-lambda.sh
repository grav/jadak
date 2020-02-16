#!/usr/bin/env bash

set -euxo pipefail

package_dir=$(mktemp -d)

runtime="arn:aws:lambda:eu-west-1:313836948343:layer:lumo-runtime:20"
role="arn:aws:iam::313836948343:role/lambda-role"
fname=jadak-serverless-example
zipfile="$(mktemp).zip"

cp -r serverless "$package_dir"
mkdir "$package_dir/lib"
cp -r ~/.m2/repository/bidi/bidi/2.1.6/bidi-2.1.6.jar "$package_dir/lib"
cp -r ../src/jadak "$package_dir"

tree "$package_dir"

( cd "$package_dir" && zip -qr "$zipfile" . )

#aws lambda delete-function --function-name $fname 2> /dev/null || true
aws lambda update-function-code --function-name $fname --zip-file "fileb://$zipfile"
aws lambda update-function-configuration --function-name $fname --layers "$runtime" \
--runtime provided --role $role --handler serverless.core/aws-lambda-main \
 --memory-size 512 --timeout 30

