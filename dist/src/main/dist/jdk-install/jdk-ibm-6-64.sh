#!/bin/bash

set -e

source jdk-support.sh

install wget

cd ~
wget --no-verbose http://ec2-54-87-52-100.compute-1.amazonaws.com/ibm-java-x86_64-sdk-6.0-15.1.bin
chmod +x ibm-java-x86_64-sdk-6.0-15.1.bin
sudo ./ibm-java-x86_64-sdk-6.0-15.1.bin -i silent

prepend 'export PATH=$JAVA_HOME/bin:$PATH'  ~/.bashrc
prepend "export JAVA_HOME=/opt/ibm/java-x86_64-60/"  ~/.bashrc
