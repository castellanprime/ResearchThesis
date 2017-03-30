#!/bin/sh

MQ_PATH="/usr/local/share/java/zmq.jar"
ZMQ_LIB_PATH="/usr/local/lib"
PROG_PATH=".:"

TOO_MANY_ARGUMENTS="Too many arguments"

usage(){
	echo "Usage: ./config.sh [ -r | -c ] [ <class> | <class>.java <class> ]"
}

run(){
	if (( $# > 2 )); then
		echo $TOO_MANY_ARGUMENTS
	fi

	if (( $#  ))

}
