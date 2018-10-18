#!/bin/bash
echo "*************************"
echo "* Installing Python 3.6 *"
echo "*************************"
sudo yum install -y python36 git

echo "******************"
echo "* Installing tox *"
echo "******************"
pip install --user --upgrade tox
