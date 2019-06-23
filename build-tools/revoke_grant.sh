#!/bin/bash

set -e

if [ ! -f build-tools/grant_id.txt ]; then
    >&2 echo "No grant ID found to revoke! (Do you need to 'assign-grant' first?)"
    exit 1
fi

echo "********************"
echo "* Revoking Grants  *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
alias="alias/busy-engineers-workshop-us-west-2-key"
arn="arn:aws:kms:us-west-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --region "us-west-2" --query 'KeyMetadata.KeyId' --output text)
grant_id=$(<build-tools/grant_id.txt)
revoke=$(aws kms revoke-grant --key-id $key_id --region "us-west-2" --grant-id $grant_id)
echo "grant_id successfully revoked...$revoke"
rm build-tools/grant_id.txt
