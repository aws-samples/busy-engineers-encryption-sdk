#!/bin/bash

# Copyright 2018-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
# the License. A copy of the License is located at
#
#     http://aws.amazon.com/apache2.0/
#
# or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
# CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
# and limitations under the License.

echo "********************"
echo "* Installing pyenv *"
echo "********************"
sudo yum install -y zlib-devel bzip2 bzip2-devel readline-devel sqlite sqlite-devel openssl-devel xz xz-devel libffi-devel

# Install pyenv
git clone https://github.com/pyenv/pyenv.git $HOME/.pyenv

# Set up bash_profile for later
echo '
## pyenv configs
export PYENV_ROOT="$HOME/.pyenv"
export PATH="$PYENV_ROOT/bin:$PATH"

if command -v pyenv 1>/dev/null 2>&1; then
  eval "$(pyenv init -)"
fi
' >> ~/.bashrc

# Set up pyenv for this shell
. ~/.bashrc

echo "*************************"
echo "* Installing Python 3.6 *"
echo "*************************"
pyenv install --skip-existing 3.6.1
pyenv local 3.6.1

echo "*****************"
echo "* Upgrading pip *"
echo "*****************"
sudo pip install --upgrade pip

echo "******************"
echo "* Installing tox *"
echo "******************"
pip install --user --upgrade tox tox-pyenv
