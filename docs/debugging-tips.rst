.. _Debugging Tips:

**************
Debugging Tips
**************

.. _CloudWatch Logs Filters:

CloudWatch Logs Filters
***********************

KMS CMK Usage
=============

The application provides some filters to find events for your KMS CMK ARN. Here are some additional filters to explore
KMS calls in the application to understand how the key is being used.

Note that these filters will show all matching events, including for any other KMS keys in your account that are not
being used in this workshop. If you want to scope it down to just the workshop's ARN,
add an additional constraint like the example KMS CMK constraint specified below.

|cloudwatch_logs|

.. |cloudwatch_logs| raw:: html

    <a href="https://us-east-2.console.aws.amazon.com/cloudwatch/home?region=us-east-2#logs:"
    target="_blank">Open the CloudWatch Logs console.</a>


* Filter for ``GenerateDataKey`` events: ``{ $.eventName = "GenerateDataKey" }``
* Filter for ``Encrypt`` events: ``{ $.eventName = "Encrypt" }``
* Filter for ``Decrypt`` events: ``{ $.eventName = "Decrypt" }``
* Filter for events for a specific KMS CMK: ``{ $.resources[0].ARN = "arn:aws:kms:us-east-2:<account>:key/<key>" }``
    * Be sure to substitute the identifiers for account and key appropriately
* Tip: use boolean operators ``&&`` and ``||`` to combine clauses


Lambda Logs
===========

If your Lambda is not behaving as you expect after deploying your updated application, you can find logs emitted by the
Lambda |lambda_logs|.

.. |lambda_logs| raw:: html

    <a href="https://us-east-2.console.aws.amazon.com/cloudwatch/home?region=us-east-2#logs:prefix=/aws/lambda"
    target="_blank">in the CloudWatch Logs log group prefix of /aws/lambda</a>

.. _Python Sanity Check:

Python Sanity Check
*******************

To sanity check your Python application before deployment, use ``flake8`` to do a basic sanity check of syntax,
style, and imports.

.. code-block:: bash

    tox -e flake8
