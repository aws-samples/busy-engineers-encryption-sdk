# A Busy Engineer's Guide to the AWS Encryption SDK

In this workshop, you will add encryption and decryption features to a simple web application that
sends form data over Amazon SQS. 

First, you'll enable encryption by calling KMS directly, then, you'll change your code to use the
Encryption SDK to call KMS for you. You will also add data key caching to reduce calls to KMS call
overhead and an encryption context. The encryption context is non-secret data that you can use for
tracking and auditing, verifying the identity of an encrypted message, and as a condition for grants
and policies.

# Getting started

Instructions on how to set up (and to shut down) the example application, as well as the actual exercises
can be [found here](http://busy-engineers-guide.reinvent-workshop.com), or in the 'exercises' branch of this repository.

# License information

Copyright 2017-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
the License. A copy of the License is located at

  http://aws.amazon.com/apache2.0/

or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
and limitations under the License.
