#!/bin/bash

echo "********************"
echo "* Revoking Grant *"
echo "********************"

account=$(aws sts get-caller-identity --query 'Account' --output text)
account_arn=$(aws sts get-caller-identity --query 'Arn' --output text)
alias="alias/busy-engineers-encryption-sdk-key-us-east-2-eek"
arn="arn:aws:kms:us-east-2:$account:$alias"
key_id=$(aws kms describe-key --key-id $arn --query 'KeyMetadata.KeyId' --output text)
echo $key_id

#fill the quotations in with the grant-id you got from when you assigned the grant
revoke=$(aws kms revoke-grant --key-id $key_id --grant-id "")
echo $revoke