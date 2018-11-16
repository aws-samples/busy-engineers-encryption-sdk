.. _Cleaning up:

***********
Cleaning up
***********

When you are finished working with your environment, here's how to completely clean up the resources in your account.

.. _Cleaning up the web application:

Cleaning up the web application
===============================

.. tabs::

    .. group-tab:: Java

        When you are finished with the workshop, this will destroy the web application stack and clean up:

        .. code-block:: bash

            mvn deploy -Pdestroy

    .. group-tab:: Python

        When you are finished with the workshop, this will destroy the web application stack and clean up:

        .. code-block:: bash

            tox -e destroy

Cleaning up your development environment
========================================

.. tabs::

    .. group-tab:: Cloud9

        The CloudFormation templates in :ref:`Environment Setup` create three CloudFormation stacks to clean up.

        Use the |cloud9_console_link| to clean up the stacks created for your Cloud9 setup.

        .. |cloud9_console_link| raw:: html

            <a href="https://us-east-2.console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks?filter=active" target="_blank">
            CloudFormation console</a>

        They'll be named as follows. Delete them in this order:

        #. ``aws-cloud9-BusyEngineersEncryptionSDK-<string>``
        #. ``BusyEngineersSdkCloud9``
        #. ``BusyEngineersSdkBase``

    .. group-tab:: Manual

        Open `the EC2 console for us-east-2
        <https://us-east-2.console.aws.amazon.com/ec2/v2/home?region=us-east-2#Instances:sort=instanceId>`_.

        Terminate the instance you created earlier.

        Go to `the IAM console's Roles section <https://console.aws.amazon.com/iam/home?region=us-east-2#/roles>`_.

        Remove the ``busy-engineers-workshop-admin`` role.