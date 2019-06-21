#!/bin/bash

echo "********************"
echo "* Assigning Grants *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
account_arn=$(aws iam list-roles | grep "Lambda" | grep "arn" | awk '{print $2}' | tr -d \")
alias="alias/busy-engineers-workshop-us-west-2-key"
arn="arn:aws:kms:us-west-2:$account:$alias"
key_id=$(aws kms describe-key --region "us-west-2" --key-id $arn --query 'KeyMetadata.KeyId' --output text)
grant_id=$(aws kms create-grant --region "us-west-2" --key-id $key_id --operations "Encrypt" "Decrypt" \
"CreateGrant" "RetireGrant" --grantee-principal $account_arn --query 'GrantId' --output text)
echo $grant_id >> build-tools/grant_id.txt
echo "grant successfully assigned..."