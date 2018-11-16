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
# webapp-python, send it to a remote server, build it
# there, package it as a zip file for Lambda, and download
# that zip file back to this host.
#
#Prerequisites:
# 1. Python 3 available locally.
# 2. SSH-accessible EC2 instance running Amazon Linux
#   NOTE: This needs to be a recent version of Amazon Linux,
#    so I recommend just creating a new instance for this
#    workshop.
# 3. SSH key that grants access to above host.
#
#Usage:
# ./python-build-remote.sh $HOST_NAME $SSH_KEY
#
#Output:
#You will see all of the normal output from the commands
# that this script runs both on your local system and on
# the remote host. You can mostly ignore these.

USER='ec2-user'
# 1 : host
HOST=${1?Host name must be provided}
# 2 : key
KEY=${2?Key must be provided}

DIR='busy_engineers_workshop_artifacts'
FILENAME='busy_engineers_workshop_python.zip'

rm -rf ${DIR}
mkdir ${DIR}
ssh -i ${KEY} ${USER}@${HOST} "rm -rf ${DIR}; mkdir ${DIR}"
scp -i ${KEY} build-tools/python-build.sh ${USER}@${HOST}:~/${DIR}/
scp -i ${KEY} dist/* ${USER}@${HOST}:~/${DIR}/
ssh -i ${KEY} ${USER}@${HOST} "chmod 755 ${DIR}/python-build.sh; ./${DIR}/python-build.sh"
scp -i ${KEY} ${USER}@${HOST}:~/${DIR}/${FILENAME} ${DIR}/${FILENAME}
