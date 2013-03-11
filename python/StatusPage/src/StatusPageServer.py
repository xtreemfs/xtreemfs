#!/usr/bin/env python
#
# Copyright (c) 2013 by Christoph Kleineweber,
#               Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

import subprocess, threading, sys, time, BaseHTTPServer

hosts = ["tick.zib.de", "trick.zib.de", "track.zib.de"]
hostStatus = {}
hostStatusLock = threading.Lock()

def isReachable(host):
    ret = subprocess.call("fping -t 100 %s" % host, 
        shell=True, 
        stdout=open('/dev/null', 'w'), 
        stderr=subprocess.STDOUT)
    if ret == 0:
        return True 
    else:
        return False

class MonitoringThread(threading.Thread):
    def __init__(self, hosts):
        threading.Thread.__init__(self)
        self.hosts = hosts
        self.runThread = True
        
    def run(self):
        while self.runThread:
            for host in self.hosts:
                reachable = isReachable(host)
                hostStatusLock.acquire()
                hostStatus[host] = reachable
                hostStatusLock.release()
            time.sleep(0.1)
            
    def stop(self):
        self.runThread = False

class StatusHTTPRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def generateStatusOutput(self):
        hostStatusLock.acquire()
        result = "# hostname status\n"

        for key in hostStatus.keys():
            result += key + "\t"
            if hostStatus[key] == True:
                result += "online"
            else:
                result += "offline"
            result += "\n"

        hostStatusLock.release()
        return result

    def do_GET(self):
        try:
            if(self.path == "/status"):
                self.send_response(200)
                self.send_header('Content-type', 'text/plain; charset=utf-8')
                self.end_headers()
                self.wfile.write(self.generateStatusOutput())
                return
            elif(self.path == "/d3.v3.js"):
                self.send_response(200)
                self.send_header('Content-type', 'text/plain; charset=utf-8')
                self.end_headers()
                f = open('.' + self.path, "r")
                self.wfile.write(f.read())
                return
            else:
                self.send_response(200)
                self.send_header('Content-type', 'text/html; charset=utf-8')
                self.end_headers()
                f = open("status.html", "r")
                self.wfile.write(f.read())
                return
        except IOError:
            self.send_error(404, 'Error')

def main():
    try:
        monitoringThread = MonitoringThread(hosts)
        monitoringThread.start()
        webserver = BaseHTTPServer.HTTPServer(('', 8080), StatusHTTPRequestHandler)
        print 'starting webserver'
        webserver.serve_forever()
    except KeyboardInterrupt:
        monitoringThread.stop()
        webserver.socket.close()
        sys.exit(0)

if __name__ == "__main__":
    main()
