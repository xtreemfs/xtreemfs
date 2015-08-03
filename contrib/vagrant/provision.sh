#!/usr/bin/env bash

echo "Installing XtreemFS build dependencies"
apt-get -y update
apt-get -y install openjdk-7-jdk ant build-essential libssl-dev libfuse-dev libattr1-dev cmake libboost-regex-dev libboost-program-options-dev libboost-thread-dev libboost-system-dev valgrind
echo "export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64" >> /etc/bash.bashrc
echo "export BUILD_CLIENT_TESTS=true" >> /etc/bash.bashrc
