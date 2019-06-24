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
"""Python 3.6 Lambda for Busy Engineer's Guide to the AWS Encryption SDK workshop."""
from setuptools import find_packages, setup

setup(
    name="busy_engineers_workshop",
    version="0.0.3",
    packages=find_packages("src"),
    package_dir={"": "src"},
    license="Apache License 2.0",
    install_requires=["aws_encryption_sdk>=1.3.8"],
)
