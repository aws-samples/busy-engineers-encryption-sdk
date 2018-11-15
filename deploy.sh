#!/bin/bash

set -x
set -u
set -e

BUCKET=busy-engineers-guide.reinvent-workshop.com
CFDISTRO=E1YZZI7YTR2LFY

aws s3 sync --acl public-read docs/build/html s3://$BUCKET

aws configure set preview.cloudfront true
aws cloudfront create-invalidation --distribution-id $CFDISTRO --paths '/*'
