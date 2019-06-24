
.. _Environment Setup:

*****************
Environment Setup
*****************

Here are instructions to prepare your environment to run the workshop.

.. _Setup a workspace:

.. tabs::

    .. group-tab:: AWS Classroom

        If you are participating in the workshop in a classroom setting at an AWS-sponsored event, your classroom account will have the bootstrap IAM permissions already.

    .. group-tab:: Your Own Account

        If you plan to launch the workshop CloudFormation stacks in your own AWS account, this CloudFormation stack will bootstrap required permissions for you.

        **Important:** Don't forget to check the acknowledgement that CloudFormation will be creating IAM resources for you.

        |launch_permissions_cfn|

        .. |launch_permissions_cfn| raw:: html

            <a href="https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=busy-engineers-ee-iam&templateURL=https://s3.amazonaws.com/busy-engineers-guide.reinvent-workshop.com/cloudformation/busy-engineers-encryption-sdk-iam.yaml"
                target="_blank">

        .. image:: ./images/cloudformation-launch-stack.png

        |launch_permissions_cfn_link_close|

        .. |launch_permissions_cfn_link_close| raw:: html

            </a>

.. tabs::

    .. group-tab:: Cloud9

        We have provided CloudFormation templates that creates a Cloud9 workspace with all of the prerequisites to work with the workshop code and system.

        The setup is split across two stacks: the Initial Stack, for basic resources, and the Cloud9 stack, for an IDE.

        **Initial Stack**

        Sign into the Console and launch the initial CloudFormation stack.

        |launch_initial_cfn|

        .. |launch_initial_cfn| raw:: html

            <a href="https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=BusyEngineersSdkBase&templateURL=https://s3.amazonaws.com/busy-engineers-guide.reinvent-workshop.com/cloudformation/busy-engineers-encryption-sdk.yaml"
                target="_blank">

        .. image:: ./images/cloudformation-launch-stack.png

        |launch_initial_cfn_link_close|

        .. |launch_initial_cfn_link_close| raw:: html

            </a>

        **Cloud9 Stack**

        Wait for the initial CloudFormation stack deployment to complete, then launch the Cloud9 CloudFormation stack.

        |launch_cloud9_cfn|

        .. |launch_cloud9_cfn| raw:: html

            <a href="https://console.aws.amazon.com/cloudformation/home?region=us-east-2#/stacks/new?stackName=BusyEngineersSdkCloud9&templateURL=https://s3.amazonaws.com/busy-engineers-guide.reinvent-workshop.com/cloudformation/busy-engineers-encryption-sdk-cloud9.yaml"
                target="_blank">

        .. image:: ./images/cloudformation-launch-stack.png

        |launch_cloud9_cfn_link_close|

        .. |launch_cloud9_cfn_link_close| raw:: html

            </a>

.. _Prerequisites Installation:

Prerequisites Installation
==========================

.. tabs::

    .. group-tab:: Cloud9 (Java)

        Use the console to find your |cloud9_java|.

        .. |cloud9_java| raw:: html

            <a href="https://us-east-2.console.aws.amazon.com/cloud9/home?region=us-east-2" target="_blank">Cloud9 IDE</a>

        After you're logged in to the Cloud9 IDE, use our utility script to prepare your language environment.

        You will be prompted to choose the default version of Java during the install process. Choose Java 1.8.x.

        .. code-block:: bash

            cd busy-engineers-encryption-sdk
            git checkout utilities
            ./prep-java.sh

        .. attention::

            Cloud9 should automatically check out the git repository when you activate the IDE. Sometimes this script
            does not run. If you do not have a copy of ``busy-engineers-encryption-sdk``, close your IDE tab and reopen it.

    .. group-tab:: Cloud9 (Python)

        Use the console to find your |cloud9_python|.

        .. |cloud9_python| raw:: html

            <a href="https://us-east-2.console.aws.amazon.com/cloud9/home?region=us-east-2" target="_blank">Cloud9 IDE</a>

        After you're logged in to the Cloud9 IDE, use our utility script to prepare your language environment.

        .. code-block:: bash

            cd busy-engineers-encryption-sdk
            git checkout utilities
            ./prep-python.sh
            . ~/.bashrc

        .. attention::

            Cloud9 should automatically check out the git repository when you activate the IDE. Sometimes this script
            does not run. If you do not have a copy of ``busy-engineers-encryption-sdk``, close your IDE tab and reopen it.

At this point you should have a Linux system that can deploy the example application with the instructions in
:ref:`Exercise 1`.

.. _AWS CLI documentation: https://docs.aws.amazon.com/cli/index.html
.. _The AWS CLI: https://docs.aws.amazon.com/cli/index.html
.. _Git: https://git-scm.com/
.. _Maven 3: https://maven.apache.org/