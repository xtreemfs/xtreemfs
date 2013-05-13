#!/usr/bin/env python
#
# Copyright (c) 2013 by Christoph Kleineweber,
#               Zuse Institute Berlin
#
# Licensed under the BSD License, see LICENSE file for details.

import threading, sys, time, BaseHTTPServer

from urllib2 import urlopen, URLError, HTTPError
import socket
import json


hosts = ["localhost:30640", "localhost:30641", "localhost:30642",  "localhost"]
hostStatus = {}
hostStatusLock = threading.Lock()


def getStatus(host):
    try:
        url = "http://{host}/rft.json".format(host=host)
        f = urlopen(url, timeout=0.1)
        raw = f.read()
        data = json.loads(raw)
        f.close()

        return data

    except ValueError as e:
        return None
    except HTTPError as e:
        return None
    except URLError as e:
        if isinstance(e.reason, socket.timeout) or isinstance(e.reason, socket.error):
            return False
        return None
    # TODO: remove this except
    except socket.timeout as e:
        return False




def statusToString(status):
    if status == False:
        return "offline"
    elif status == None:
        return "unknown"
    else:
        return "online"


def statusToRole(status, file_id):
    if status == False:
        return "offline"
    elif status == None:
        return "unknown"

    # empty file list
    elif type(status) == list:
        return "unknown"

    print("status:" + str(status))
    print("file_id:" + file_id)

    if file_id in status and "role" in status[file_id]:
        role = status[file_id]["role"]
        if role.startswith("primary"):
            return "primary"
        elif role.startswith("backup"):
            return "backup"
        # elif role.startswith("outdated"):
        #     return "outdated"
    
    return "unknown"


class MonitoringThread(threading.Thread):
    def __init__(self, hosts):
        threading.Thread.__init__(self)
        self.hosts = hosts
        self.runThread = True
        
    def run(self):
        while self.runThread:
            for host in self.hosts:
                status = getStatus(host)

                hostStatusLock.acquire()
                hostStatus[host] = status
                hostStatusLock.release()
            time.sleep(0.1)
            
    def stop(self):
        self.runThread = False

class StatusHTTPRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def generateStatusOutput(self):
        hostStatusLock.acquire()

        host_status = [ statusToString(hostStatus[host]) for host in hosts ]

        file_ids = { file_id  for host in hosts if type(hostStatus[host]) == dict for file_id in hostStatus[host].keys() }
        file_status = [ (file_id, [statusToRole(hostStatus[host], file_id) for host in hosts]) for file_id in file_ids ]

        hostStatusLock.release()

        return json.dumps({"status": host_status, "files": dict(file_status)})

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
