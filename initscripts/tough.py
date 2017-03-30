#!/usr/bin/env python

"""
	Config script for cluster

"""

import argparse
import sys
import subprocess

ZMQ_PATH="/usr/local/share/java/zmq.jar"
ZMQ_LIB_PATH="/usr/local/lib"
CURR=".:"

def check_args(args=None):
	compclass, runclass = None, None
	parser = argparse.ArgumentParser(description='Config script for cluster')
	group = parser.add_mutually_exclusive_group()
	group.add_argument('-r', '--run', help='run program' type=str, required='True', action='store_true', nargs='+')
  	group.add_argument('-c', '--complie', help='complie and run program', type=str, required='True', action='store_true', nargs='+')
  	results = parser.parse_args(args)
  	if results.run and len(results.run) >= 1:
  		run(results.run[0:1])
  	else if results.complie and len(results.complie) >= 2:
  		compclass, runclass = results.complie[0:2]
  	return runclass, compclass

def run(st):
	#subprocess.Popen(['java', '-cp', '.:/usr/local/share/java/zmq.jar', '-Djava.library.path=/usr/local/lib', st], stdout=PIPE, stderr=PIPE)]

	subprocess.call(['java', '-cp', '.:/usr/local/share/java/zmq.jar', '-Djava.library.path=/usr/local/lib', st])


def complie(st1, st2):
	subprocess.call(['java', '-cp', '.:/usr/local/share/java/zmq.jar', '-Djava.library.path=/usr/local/lib', st])