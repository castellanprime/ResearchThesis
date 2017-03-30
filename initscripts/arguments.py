#!/usr/bin/env python

"""
Example output

./arguments.py --command=init --servers=5 
./arguments.py --command=start --all=No--pid=456, 467, 235
./arguments.py --command=stop --all=No --pid=456, 467, 235
./arguments.py --command=query --all=No --pid=456, 467, 255
"""

import argparse
import sys

def check_args(args=None):
  parser = argparse.ArgumentParser(description='Script to learn basic argparse')
  group = parser.add_mutually_exclusive_group()
  parser.add_argument('-c', '--command', type=str, help='command to run', required='True')
  group.add_argument('-a', '--all', help='all the servers', action='store_true')
  group.add_argument('-p', '--pid', help='process id of the server(s)', type=str, nargs='+', default=None)
  parser.add_argument('-s', '--server', help='number of servers', type=int, default=0)
  results = parser.parse_args(args)
  return results.command, results.all, results.pid[0].split(","), results.server

def init(num=0):
  pass

def start(allproc=False, *args):
  pass

def stop(allproc=False, *args):
  pass

def query(allproc=False, *args):
  pass


"""
class BootStrap:

  def _config(self, numServers=0):
    print()
    pass

  def _stop(self, allproc=False, *args):
    pass

  def _start(self, allproc=False, *args):
    pass

  def _query(self, allproc=False, *args):
    pass

  def _error(self):
    print("Command does not exist")
    sys.exit(-1)

  def dispatch(self, name, *args):
    cmdname = '_' + name
    if hasattr(self, cmdname):
      method = getattr(self, cmdname)
      method(args)
    else:
      self._error()
"""




if __name__=='__main__':
  command, al, pids, server = check_args(sys.argv[1:])
  print('command = ', command)
  print('all = ', al)
  print('pid = ', pids)
  print('server = ', server)


