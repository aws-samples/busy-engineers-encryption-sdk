#!/bin/bash
echo "******************************"
echo "* Installing Java 8 Open JDK *"
echo "******************************"
sudo yum install -y java-1.8.0-openjdk-devel git

clear
echo ""
echo ""
echo ""
echo "**************************************************"
echo "*                       JAVA                     *"
echo "* Please select Java 8 from the below selections *"
echo "**************************************************"
echo ""
echo ""
echo ""
sudo /usr/sbin/alternatives  --config java

echo ""
echo ""
echo ""
echo "**************************************************"
echo "*                       JAVAC                    *"
echo "* Please select Java 8 from the below selections *"
echo "**************************************************"
echo ""
echo ""
echo ""
sudo /usr/sbin/alternatives  --config javac

sudo wget http://sid345.reinvent-workshop.com/software/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/7/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
mvn --version