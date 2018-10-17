#!/bin/bash

set -x
set -u
set -e

BUCKET=sid345.reinvent-workshop.com
CFDISTRO=E11MLVAL7KJ1N2

aws s3 sync --acl public-read docs/build/html s3://$BUCKET

aws configure set preview.cloudfront true
aws cloudfront create-invalidation --distribution-id $CFDISTRO --paths '/*'
