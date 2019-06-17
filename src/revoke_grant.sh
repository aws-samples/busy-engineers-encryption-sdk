#!/bin/bash

echo "********************"
echo "* Revoking Grants *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
account_arn=$(aws sts get-caller-identity --query 'Arn' --output text)
alias="alias/busy-engineers-workshop-python-key-us-east-2"
arn="arn:aws:kms:us-east-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --query 'KeyMetadata.KeyId' --output text)
echo $key_id


revoke=$(aws kms revoke-grant --key-id $key_id --grant-id "")
echo $revoke
