#!/bin/bash

echo "********************"
echo "* Assigning Grant *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
account_arn=$(aws sts get-caller-identity --query 'Arn' --output text)
alias="alias/busy-engineers-encryption-sdk-key-us-east-2-eek"
arn="arn:aws:kms:us-east-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --query 'KeyMetadata.KeyId' --output text)
echo $key_id
grant_id=$(aws kms create-grant --key-id $key_id --operations "Encrypt" "Decrypt" "GenerateDataKey" "DescribeKey" \
"CreateGrant" "RetireGrant" --grantee-principal $account_arn --constraints EncryptionContextEquals={KeyUsage="Bad Key"} --query 'GrantId' --output text)
echo $grant_id