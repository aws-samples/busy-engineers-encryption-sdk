#!/bin/bash

set -x
set -u
set -e

BUCKET=sid345.reinvent-workshop.com
CFDISTRO=E11MLVAL7KJ1N2

aws s3 sync --acl public-read site s3://$BUCKET
(cd site; for i in */index.html; do
    aws s3 cp --acl public-read --website-redirect /$i $i s3://$BUCKET/`dirname $i`
done)

aws configure set preview.cloudfront true
aws cloudfront create-invalidation --distribution-id $CFDISTRO --paths '/*'
