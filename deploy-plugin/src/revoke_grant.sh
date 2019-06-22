#!/bin/bash

echo "********************"
echo "* Revoking Grants *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
alias="alias/busy-engineers-workshop-us-west-2-key"
arn="arn:aws:kms:us-west-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --region "us-west-2" --query 'KeyMetadata.KeyId' --output text)
grant_id=$(<grant_id.txt)
revoke=$(aws kms revoke-grant --key-id $key_id --region "us-west-2" --grant-id $grant_id)
echo "grant_id successfully revoked...$revoke"
rm -rf grant_id.txt