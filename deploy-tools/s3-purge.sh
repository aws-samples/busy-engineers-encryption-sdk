#!/usr/bin/env bash
# Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

# 1 : config file
CONFIG=${1?Config file must be provided}
# 2 : static assets directory
STATIC_ASSETS=${2?Static assets directory must be provided}

BUCKET=$(python ./deployer.py bucket-name --config ${CONFIG})

aws s3 sync ${STATIC_ASSETS} s3://${BUCKET}
