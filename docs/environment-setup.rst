
.. _Environment Setup:

*****************
Environment Setup
*****************

Here are instructions to prepare your environment to run the workshop.

.. _Setup a workspace:

.. tabs::

    .. group-tab:: Cloud9

        We have created CloudFormation templates that will set up a Cloud9 workspace that has the necessary prerequisites installed
        and uses a role with the necessary permissions.

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


    .. group-tab:: Manual

        **Prerequisites**

        Install and set up the required components:

        * An AWS account
        * `The AWS CLI`_
        * `Git`_
        * ``JDK 1.8`` (Java only)
        * `Maven 3`_ (Java only)
        * Python 3.6 (we specifically need 3.6 to build binaries for Lambda) (Python only)
        * tox (Python only)

        The EC2 quickstart section will walk you through setting these up.

        **AWS credentials**

        You'll need to configure your default AWS credentials to have *Administrator* access.

        We'll be deploying a Lambda application, so we'll need to create an IAM role for it to use when talking to AWS
        services, and this requires Administrator access - Power User is not sufficient.

        The roles created are restricted to only having access to the specific resources created as part
        of this demo.

        For help setting credentials with the CLI, see the `AWS CLI documentation`_.

        .. _EC2 quickstart:

        **EC2 quickstart**

        If you'd like to set up an EC2 instance that is set up to use as a development
        environment for this demo, here's some quickstart instructions to get you set
        up.

        If you've already got a working development environment, feel free to skip
        ahead to the next section where we'll deploy the sample application.

        **Configuring and launching the instance**

        First, create an Administrator access role that the EC2 instance will use to
        access your account.

        .. warning::

            Because this is granting a high level of privileges to the instance,
            we recommend doing this in a test account.

        #. Log in to the AWS Console.
        #. Go to `the IAM console's Roles section <https://console.aws.amazon.com/iam/home?region=us-east-2#/roles>`_.
        #. Click the "Create Role" button.
        #. Under "Choose the service that will use this role", select "EC2",
           then select "EC2" for the use case and proceed to the next page.
        #. Select ``AdministratorAccess``, and proceed to the next page.
        #. Set some easy-to-remember name for the role such as "busy-engineers-workshop-admin".

        Now that you have a role created, we'll deploy a Linux instance to use as our
        launching point.

        Open `the EC2 console for us-east-2
        <https://us-east-2.console.aws.amazon.com/ec2/v2/home?region=us-east-2#Instances:sort=instanceId>`_.

        If you have not launched any instances here before, you'll first need to either
        `create a new key pair
        <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#having-ec2-create-your-key-pair>`_
        or `import an existing ssh key
        <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html#how-to-generate-your-own-key-and-import-it-to-aws>`_
        using the instructions at those links.

        Once you have the key pair set up, we can launch an instance.

        #. Click the blue 'Launch Instance' button.
        #. Select the 'Amazon Linux AMI 2018.03.0 (HVM), SSD Volume Type' AMI.
        #. Click 'Configure Instance Details' and make sure 'Auto-assign Public IP' is **Enabled**.
        #. **In 'IAM Role', select the role you created above.** ("busy-engineers-workshop-admin", or your preferred name)
        #. Click 'Review and Launch'.
        #. Click 'Launch'.
        #. In the provided dialog, select the keypair you just created or imported.
        #. Click 'Launch Instances'.

        Once the instance launches, you'll see it in the `instance list
        <https://us-east-2.console.aws.amazon.com/ec2/v2/home?region=us-east-2#Instances>`_.

        Copy the public DNS hostname. You can then log into this instance using
        username ``ec2-user`` and the keypair you created before.

        If this is your first time using EC2, see the `EC2 getting started documentation
        <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html>`_ for more detail.


.. _Prerequisites Installation:

Prerequisites Installation
==========================

.. tabs::

    .. group-tab:: Cloud9 (Java)

        Use the console to find your |cloud9_java|.

        .. |cloud9_java| raw:: html

            <a href="https://us-east-2.console.aws.amazon.com/cloud9/home?region=us-east-2" target="_blank">Cloud9 IDE</a>

        Once you're logged in to the Cloud9 IDE, use our utility script to prepare your language environment.

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

        Once you're logged in to the Cloud9 IDE, use our utility script to prepare your language environment.

        .. code-block:: bash

            cd busy-engineers-encryption-sdk
            git checkout utilities
            ./prep-python.sh
            . ~/.bashrc

        .. attention::

            Cloud9 should automatically check out the git repository when you activate the IDE. Sometimes this script
            does not run. If you do not have a copy of ``busy-engineers-encryption-sdk``, close your IDE tab and reopen it.

    .. group-tab:: Manual (Java)

        Once you're logged in, use ``yum`` to upgrade Java and install git:

        .. code-block:: bash

            sudo yum install java-1.8.0-openjdk-devel git

        Use ``alternatives`` to ensure your new Java version is the default as follows:

        .. code-block:: bash

            sudo /usr/sbin/alternatives  --config java

        For example:

        .. code-block:: bash

            [ec2-user@ip-172-31-2-67 ~]$ sudo /usr/sbin/alternatives  --config java

            There is 1 program that provides 'java'.

            Selection    Command
            -----------------------------------------------
            *+ 1           java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.181-3.b13.amzn2.x86_64/jre/bin/java)

            Enter to keep the current selection[+], or type selection number

        At the prompt select the number corresponding to 1.8.0 (``1`` here).

        Next we'll fetch Maven:

        .. code-block:: bash

            wget https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz
            wget https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/apache-maven-3.5.2-bin.tar.gz.sha1

        Since Maven uses an unsecured connection to download the Maven binaries themselves, it's good practice to check the hash of the binaries:

        .. code-block:: bash

            [ec2-user@ip-10-0-0-137 ~]$ sha1sum apache-maven-3.5.2-bin.tar.gz; cat apache-maven-3.5.2-bin.tar.gz.sha1; echo
            190dcebb8a080f983af4420cac4f3ece7a47dd64  apache-maven-3.5.2-bin.tar.gz
            190dcebb8a080f983af4420cac4f3ece7a47dd64

        Make sure the two hashes match before proceeding.

        Once you've verified the integrity of maven, we'll need to unpack it and add it to our path:

        .. code-block:: bash

            tar xzvf apache-maven-3.5.2-bin.tar.gz
            PATH=$PWD/apache-maven-3.5.2/bin:$PATH
            echo "PATH=$PWD/apache-maven-3.5.2/bin:$PATH" >> ~/.bash_profile

        Now, check out the application on your local computer:

        .. code-block:: bash

            git clone https://github.com/aws-samples/busy-engineers-encryption-sdk.git
            cd busy-engineers-encryption-sdk

        To edit files, the ``nano`` editor is built-in. You can also install or use another editor of your choice,
        such as ``vim`` or ``emacs``.

    .. group-tab:: Manual (Python)

        One you're logged in, use ``yum`` to install Python 3.6 and git:

        .. code-block:: bash

            sudo yum install python36 git

        Now install ``tox``:

        .. code-block:: bash

            python3 -m pip install --user --upgrade tox

        Now, check out the application on your local computer:

        .. code-block:: bash

            git clone https://github.com/aws-samples/busy-engineers-encryption-sdk.git
            cd busy-engineers-encryption-sdk

        To edit files, the ``nano`` editor is built-in. You can also install or use another editor of your choice,
        such as ``vim`` or ``emacs``.

At this point you should have a Linux system that can deploy the example application with the instructions in
:ref:`Exercise 1`.

.. _AWS CLI documentation: https://docs.aws.amazon.com/cli/index.html
.. _The AWS CLI: https://docs.aws.amazon.com/cli/index.html
.. _Git: https://git-scm.com/
.. _Maven 3: https://maven.apache.org/