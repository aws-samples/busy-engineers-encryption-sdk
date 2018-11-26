
.. _Main:

#################################################
A Busy Engineer's Guide to the AWS Encryption SDK
#################################################

In this workshop, you will add encryption and decryption features to a simple web application that
sends form data over `Amazon SQS`_.

First, you will enable encryption by calling KMS directly. Second, you will change your code to 
use the AWS Encryption SDK to call KMS. Third, you will add data key caching to reduce calls to KMS. 
Fourth, you will add an encryption context; non-secret data that you can use for tracking and auditing, 
verifying the identity of an encrypted message, and as a condition for grants and policies.

.. toctree::
    :maxdepth: 1

    environment-setup.rst
    exercises/1-explore.rst
    exercises/2-kms_encryption.rst
    exercises/3-encryption_sdk.rst
    exercises/4-caching.rst
    debugging-tips.rst
    cleaning-up.rst

Getting started
***************

For help setting up an environment to work through the exercises, see :ref:`Environment Setup`.

For some tips on how to debug your application if needed, see :ref:`Debugging Tips`.

When you are done experimenting, you can find instructions for cleaning up the application in :ref:`Cleaning up`.

List of exercises
*****************

* :ref:`Exercise 1` - Explore the example application and make your first change
* :ref:`Exercise 2` - Add KMS encryption to the example application
* :ref:`Exercise 3` - Add encryption using the AWS Encryption SDK
* :ref:`Exercise 4` - Add caching to the example application

Bonus Tasks
***********

If you want more challenging tasks, try these ideas:

* Downloading the `AWS Encryption SDK CLI`_ and using it to decrypt some of your messages.

* The AWS Encryption SDK supports encrypting the same message with multiple keys. Try encrypting with two KMS
  keys in different regions. Make sure you can decrypt in each region independently.

* Write your own `Cryptographic Materials Manager`_ (CMM) to transform the incoming request. For example, write one
  that adds a timestamp to the encryption context. Using the material introduced in Exercise 4, experiment with how the
  timestamp affects caching behavior and performance.

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
.. _Amazon SQS: https://aws.amazon.com/sqs/
.. _Cryptographic Materials Manager: https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/concepts.html#crypt-materials-manager