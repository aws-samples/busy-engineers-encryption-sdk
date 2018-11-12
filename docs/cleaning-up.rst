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

        Use the `CloudFormation console <https://eu-west-1.console.aws.amazon.com/cloudformation/home?region=eu-west-1#/stacks?filter=active>`_ to clean up
        the stacks created for your Cloud9 setup.

        They'll be named as follows. Delete them in this order:

        #. ``aws-cloud9-ReinventSid345Cloud9-<string>``
        #. ``MySid345Cloud9Env``
        #. ``MySid345BaseEnv``

    .. group-tab:: Manual

        Open `the EC2 console for eu-west-1
        <https://eu-west-1.console.aws.amazon.com/ec2/v2/home?region=eu-west-1#Instances:sort=instanceId>`_.

        Terminate the instance you created earlier.

        Go to `the IAM console's Roles section <https://console.aws.amazon.com/iam/home?region=eu-west-1#/roles>`_.

        Remove the ``sid345-admin`` role.