#!/bin/bash

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
