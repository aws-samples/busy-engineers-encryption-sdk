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
#This helper script builds the zip file for Lambda. See
# python-build-remote.sh for more information.

FILENAME='busy_engineers_workshop_python.zip'
DIR='busy_engineers_workshop_artifacts'

sudo yum install -y python36

cd ${DIR}
rm -rf py36test dep_dir
virtualenv py36test -p python3.6
source py36test/bin/activate
mkdir dep_dir
pip install busy_engineers_workshop-*.tar.gz -t dep_dir
deactivate
cd dep_dir
zip -r ../${FILENAME} .
cd ..
