#!/usr/bin/python
# Loop infinitly on:
#  - read BUFFER_SIZE bytes
#  - sleep for WAITFOR seconds 

import socket
import time

TCP_IP = '127.0.0.1'
TCP_PORT = 10001
BUFFER_SIZE = 4096
WAITFOR=2

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((TCP_IP, TCP_PORT))
count=0
while 1:
    data = s.recv(BUFFER_SIZE)
    count=count+1
    print "received: ", count*BUFFER_SIZE
    time.sleep(WAITFOR)

s.close()
