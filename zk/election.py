#!/usr/bin/env python2.7
import time, socket, os, uuid, sys, kazoo, logging, signal, inspect
from kazoo.client import KazooClient
from kazoo.client import KazooState
from kazoo.exceptions import KazooException

class Election:

    def __init__(self, zk, path, func,args):
        self.election_path = path
        self.zk = zk
        self.is_leader = False
        if not (inspect.isfunction(func)) and not(inspect.ismethod(func)):
            logging.debug("not a function "+str(func))
            raise SystemError
        self.id = zk.create(path+"/guid", "", None, True, True)
        zk.ChildrenWatch(self.election_path,self.ballot)
        
    def is_leading(self):
        return self.is_leader

	# perform the election
    def ballot(self,event):
        children = self.zk.get_children(self.election_path)
        children.sort()
        if self.election_path+"/"+children[0] == self.id:
            self.is_leader = True
        else:
            self.is_leader = False
        print str(self.is_leader)
                        
def Hello():
    print("hello")
    
if __name__ == '__main__':
    zkhost = "127.0.0.1:2181" #default ZK host
    logging.basicConfig(format='%(asctime)s %(message)s',level=logging.DEBUG)
    if len(sys.argv) == 2:
        zkhost=sys.argv[2]
        print("Using ZK at %s"%(zkhost))

    zk = KazooClient(zkhost)
    zk.start()
    election = Election(zk,"/election",Hello,[])
        
    while True:
        time.sleep(1)
