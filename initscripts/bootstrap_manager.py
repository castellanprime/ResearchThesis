#!/usr/bin/env python

"""
	File: bootstrap_manager.py
	Date: 21/03/2017
	Brief: Bootstrap the cluster for Raft and CRDTs
"""
import sys, os, time
from subprocess import Popen, PIPE

running_procs = []
files = []

# + str(os.getcwd()) +
# for path in files

def main(list):
	pid, number = (sys.argv[1:3])
	if int(pid) == 0:
		#files = ["./" + "bootstrap_manager.py 1 0" for i in range(int(number))]
		running_procs = [Popen(['gnome-terminal', '-x', './bootstrap_manager.py', '1', '0'], stdout=PIPE, stderr=PIPE) for i in range(3)]
		print("File")
		while running_procs:
			for proc in running_procs:
				retcode = proc.poll()
				if retcode is not None: # Process finished.
					running_procs.remove(proc)
					break
				else: # No process is done, wait a bit and check again.
					time.sleep(.1)
					continue
		if retcode != 0 and retcode != None:
			handle_results(proc.stdout)
	else:
		while True:
			user_str = input("Enter string or enter n to exit: ")
			print(user_str)
			if user_str == "n":
				break
		print("Pid of current finished process: ", os.getpid())

def handle_results(str):
	for line in str:
		print(line)

main(sys.argv)


