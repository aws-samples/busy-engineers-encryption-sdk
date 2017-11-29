""""""
from setuptools import find_packages, setup

setup(
    name='reinvent_sid345_lambda',
    version='0.0.1',
    packages=find_packages('src'),
    package_dir={'': 'src'},
    license='Apache License 2.0',
    install_requires=['aws_encryption_sdk']
)
