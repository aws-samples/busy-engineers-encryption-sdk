
.. _Main:

###########################################################
SID345: The Busy Engineer's Guide to the AWS Encryption SDK
###########################################################

In this workshop, you will add encryption and decryption features to a simple web application that
sends form data over Amazon SQS.

First, you'll enable encryption by calling KMS directly, then, you'll change your code to use the
Encryption SDK to call KMS for you. You will also add data key caching to reduce calls to KMS call
overhead and an encryption context. The encryption context is non-secret data that you can use for
tracking and auditing, verifying the identity of an encrypted message, and as a condition for grants
and policies.

.. toctree::
    :maxdepth: 1

    exercises/0-explore.rst
    exercises/1-kms_encryption.rst
    exercises/2-encryption_sdk.rst
    exercises/3-caching.rst

Getting started
***************

Instructions on how to set up (and to shut down) the example application can be
found in :ref:`Exercise 0`.

List of exercises
*****************

* :ref:`Exercise 0` - Explore the example application and make your first change
* :ref:`Exercise 1` - Add KMS encryption to the example application
* :ref:`Exercise 2` - Add encryption using the AWS Encryption SDK
* :ref:`Exercise 3` - Add caching to the example application

Additional exploration
**********************

If you find yourself wanting more after these exercises, here are some ideas you can explore:

* Try downloading the `AWS Encryption SDK CLI`_ and using it to decrypt some of your messages.

* The AWS Encryption SDK supports encrypting to multiple recipients. Try encrypting to two KMS
  keys in different regions, and make sure you can decrypt using each independently.

* You can write your own crypto materials manager to transform the incoming request. Try adding
  one that adds additional audit information (say, a timestamp) to the encryption context. If you
  place this after the cache you can avoid some of the issues you'll otherwise encounter with
  fine timestamps with caching.

License information
*******************

Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
the License. A copy of the License is located at

https://aws.amazon.com/apache2.0/

or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
and limitations under the License.


.. _AWS Encryption SDK CLI: https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/crypto-cli.html
