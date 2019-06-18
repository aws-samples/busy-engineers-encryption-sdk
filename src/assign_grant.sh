#!/bin/bash

echo "********************"
echo "* Assigning Grants *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
account_arn=$(aws sts get-caller-identity --query 'Arn' --output text)
alias="alias/busy-engineers-workshop-python-key-us-west-2-testdeploy"
arn="arn:aws:kms:us-west-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --query 'KeyMetadata.KeyId' --output text)
echo $key_id
grant_id=$(aws kms create-grant --key-id $key_id --operations "Encrypt" "Decrypt" "DescribeKey" \
"CreateGrant" "RetireGrant" --grantee-principal $account_arn --query 'GrantId' --output text)
echo "Grant Id: $grant_id"