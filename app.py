#!/usr/bin/env python

"""
	Config script for cluster

"""

import argparse
import sys
import subprocess

def check_args(args=None):
	compclass, runclass , intargs = None, None, 0
	parser = argparse.ArgumentParser(description='Config script for cluster')
	group = parser.add_mutually_exclusive_group(required='True')
	group.add_argument('-r', '--run', help='run program', nargs='*', default=[])
	group.add_argument('-c', '--complie', help='complie and run program', nargs='*', default=[])
	results = parser.parse_args(args)
	if results.run:
		runclass, intargs = results.run
	elif results.complie and len(results.complie) >= 3:
		compclass, runclass, intargs = results.complie[0:3]
	return compclass, runclass, intargs

def run(st, intargs):
	subprocess.call(['java', '-cp', '.:/usr/local/share/java/zmq.jar', '-Djava.library.path=/usr/local/lib', st, intargs])

def complie(st1, st2, intargs):
	subprocess.call(['javac', '-cp', '.:/usr/local/share/java/zmq.jar', st1])
	run(st2, inargs)

if __name__=='__main__':
	compli, runn, inargs = check_args(sys.argv[1:])
	if compli:
		complie(compli, runn, inargs)
	else:
		run(runn, inargs)
