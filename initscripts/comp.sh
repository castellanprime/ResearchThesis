#!/bin/bash

ZMQ_PATH="/usr/local/share/java/zmq.jar"
ZMQ_LIB_PATH="/usr/local/lib"
CURR=".:"


echo -n "Usage: comp <class.java> <class>"
echo "Compling"
javac -cp $ZMQ_PATH $1
echo "Running"
if [[ $# -eq 3 ]]; then
	java -cp $CURR$ZMQ_PATH -Djava.library.path=$ZMQ_LIB_PATH $2 $3
else
	java -cp $CURR$ZMQ_PATH -Djava.library.path=$ZMQ_LIB_PATH $2 
fi
