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
        result = "<table>"

        for key in hostStatus.keys():
            if hostStatus[key] == True:
                color = "green"
            else:
                color = "red"
            result = result + "<tr><td bgcolor=" + color + ">" + key + "</td></tr>"

        result = result + "</table>"
        hostStatusLock.release()
        return result

    def do_GET(self):
        try:
            if(self.path.endswith("status")):
                self.send_response(200)
                self.send_header('Content-type', 'text/html')
                self.end_headers()
                self.wfile.write(self.generateStatusOutput())
                return
            else:
                self.send_response(200)
                self.send_header('Content-type', 'text/html')
                self.end_headers()
                self.wfile.write("""
                        <html>
                            <head>
                                <title>Host Status</title>
                            </head>
                            <script>
                                function updateStatus() {
                                    var ajaxRequest;

                                    try {
                                        ajaxRequest = new XMLHttpRequest();
                                    } catch(e) {
                                        console.log(e);
                                    }
                                    ajaxRequest.open('GET', 'status', true);
                                    ajaxRequest.onreadystatechange = function() {
                                        if (ajaxRequest.readyState == 4) {
                                            var statusData = ajaxRequest.responseText;
                                            var txt = document.getElementById("status");
                                            txt.innerHTML = statusData;
                                        }
                                    };
                                    ajaxRequest.send(null);
                                    window.setTimeout(updateStatus, 1000);
                                }
                                updateStatus();
                            </script>
                            <body>
                                <div id=status></div>
                            </body>
                        </html>
                    """)
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
