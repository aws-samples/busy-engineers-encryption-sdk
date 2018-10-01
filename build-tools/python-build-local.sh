#!/usr/bin/env bash
# Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"). You
# may not use this file except in compliance with the License. A copy of
# the License is located at
#
# http://aws.amazon.com/apache2.0/
#
# or in the "license" file accompanying this file. This file is
# distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
# ANY KIND, either express or implied. See the License for the specific
# language governing permissions and limitations under the License.
#
#This helper script will package the Python project in
# webapp-python, build it locally, package it as a zip
# file for Lambda, and upload that zip file to an S3 bucket.
#
#Prerequisites:
# 1. Python 3.6 available locally.
# 2. Local environment MUST be a recent version of Amazon Linux.
# 3. Credentials available in local environment that can write
#    to the specified S3 bucket.
#
#Usage:
# ./python-build-remote.sh $BUCKET_NAME
#
#Output:
#You will see all of the normal output from the commands
# that this script runs. You can mostly ignore these. The only
# output that is necessary is the final S3 ListObjectVersions
# JSON that shows you the new VersionId of the uploaded
# file in S3.

DIR='reinvent_sid345_artifacts'

rm -rf ${DIR}
mkdir ${DIR}
cp build-tools/python-build.sh ${DIR}/
cp dist/* ${DIR}/
chmod 755 ${DIR}/python-build.sh
cd ${DIR}
python-build.sh
