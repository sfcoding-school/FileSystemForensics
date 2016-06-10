#!/usr/bin/env python
"""
Very simple HTTP server in python.
Usage::
    ./dummy-web-server.py [<port>]
Send a GET request::
    curl http://localhost
Send a HEAD request::
    curl -I http://localhost
Send a POST request::
    curl -d "foo=bar&bin=baz" http://localhost
"""
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import cgi
from threading import Thread
import socket
import sys
import Queue
import json
import os
import csv
import base64
import zipfile
import io
from SimpleHTTPServer import SimpleHTTPRequestHandler


class CORSRequestHandler (SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        SimpleHTTPRequestHandler.end_headers(self)

q = Queue.Queue()
b = Queue.Queue()


class S(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    def do_GET(self):
        self._set_headers()
        # Parse out the arguments.
        # The arguments follow a '?' in the URL. Here is an example:
        #   http://example.com?arg1=val1
        args = {}
        idx = self.path.find('?')
        if idx >= 0:
            rpath = self.path[:idx]
            args = cgi.parse_qs(self.path[idx+1:])
        else:
            rpath = self.path

        # Print out logging information about the path and args.
        if 'content-type' in self.headers:
            ctype, _ = cgi.parse_header(self.headers['content-type'])
            print('TYPE %s' % (ctype))

        print('PATH %s' % (rpath))
        print('ARGS %d' % (len(args)))
        if len(args):
            i = 0
            # pathToSend = ""

            for key in sorted(args):
                print('ARG[%d] %s=%s' % (i, key, args[key]))
                i += 1

            try:
                dir = os.path.dirname(__file__)
                print "args[cmd]", args["cmd"][0]

                if args["cmd"][0] == "list":
                    self.wfile.write(listAllDevice())
                elif args["cmd"][0] == "getjson":
                    self.wfile.write( readInfo(args["id"][0]))
                elif args["cmd"][0] == "getfs":
                    with open(dir + "/DB/" + args["id"][0]) as data_file:
                        jjj = json.load(data_file)
                        print json.dumps(jjj)
                        self.wfile.write(json.dumps(jjj))
                        
                else:
                    q.put("ls:" + args["cmd"][0])
                    print "done"
                    aaaa = b.get()
                    self.wfile.write(aaaa)
                    b.task_done()

            except Exception, e:
                print e
        else:
            self.wfile.write("<html><body><h1>bhoooo!</h1></body></html>")

    def do_HEAD(self):
        self._set_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        file_content = self.rfile.read(content_length)

        # Do what you wish with file_content

        # print file_content
        #json_data = json.loads(file_content)
        dir = os.path.dirname(__file__)

        try:
            json_data['BRAND']


            print json_data['SERIAL'] + "   " + json_data['sha1']

            if not os.path.exists(dir+'/DB/'+json_data['SERIAL']+"/"):
                print 'sono qui'
                os.makedirs(dir+'/DB/'+json_data['SERIAL'])

            if not os.path.isfile(dir+'/DB/'+json_data['SERIAL']+"/info.csv"):
                print 'creo file info'
                with open(dir+'/DB/'+json_data['SERIAL']+"/info.csv", 'w+') as menu:
                    wr1 = csv.writer(menu, dialect="excel")
                    wr1.writerow([json_data['DEVICE'], json_data['MANUFACTURER'],
                        json_data['SERIAL'], json_data['BRAND'],json_data['BUILDN']])
                menu.close()

            if not os.path.isfile(dir+'/DB/'+json_data['SERIAL']+"/"+json_data['sha1']):
                print 'aggiungo file info'
                with open(dir+'/DB/'+json_data['SERIAL']+"/info.csv", 'a') as menu:
                    wr1 = csv.writer(menu, dialect="excel")
                    wr1.writerow([json_data['sha1'], json_data['DATA']])
                menu.close()

                print 'creo il file'
                with open(dir+'/DB/'+json_data['SERIAL']+"/"+json_data['sha1'], 'w+') as outfile:
                    json.dump(json_data, outfile)
                outfile.close()


            print readInfo(json_data['SERIAL'])

            # Respond with 200 OK
            self.send_response(200)
            # # Doesn't do anything with posted data
            # self._set_headers()

            # ctype, pdict = cgi.parse_header(self.headers.getheader('content-type'))
            # print ctype, pdict
            # self.wfile.write("OK")

        except Exception, e:
            #print json_data['BYTE_ARRAY']
            print len(file_content)
            print self.headers['Content-Type']
            id_device = self.headers['id-device']
            filename = self.headers['filename']

            with open(dir+'/DB/'+id_device+'/file/'+filename +'.zip', 'wb') as output:
                output.write(file_content)


def makeDownlodableFile(path):
    #dir = os.getcwd()  
    dir = os.path.dirname(__file__)
    FILEPATH = os.path.dirname(dir) + path
    with open(FILEPATH, 'rb') as f:
        self.send_response(200)
        self.send_header("Content-Type", 'application/octet-stream')
        self.send_header("Content-Disposition", 'attachment; filename="{}"'.format(os.path.basename(FILEPATH)))
        fs = os.fstat(f.fileno())
        self.send_header("Content-Length", str(fs.st_size))
        self.end_headers()
        shutil.copyfileobj(f, self.wfile)


def listAllDevice():
    #dir = os.getcwd()
    dir = os.path.dirname(__file__)
    print "listAllDevice", dir, os.path.exists(dir + "/DB/")
    lst = os.listdir(dir + "/DB/")
    to_return = ""
    for a in lst:
        to_return += a + ":"
    return to_return


def readInfo(id_device):
    #dir = os.getcwd()  
    dir = os.path.dirname(__file__)
    print "readInfo", dir, os.path.exists(dir+'/DB/'+id_device+"/info.csv")
    try:
        if os.path.isfile(dir+'/DB/'+id_device+"/info.csv"):
            print 'carico le info'
            Flag_primo = True
            data = {}
            lista = []
            with open(dir+'/DB/'+id_device+"/info.csv") as read:
                for line in csv.reader(read, dialect="excel"):
                    if Flag_primo:
                        data['info'] = line
                        Flag_primo = False
                    else:
                        lista.append(str(line[0]+":"+line[1]))

            data['lista'] = lista
            print "readInfo:return"
            return json.dumps(data)
        else:
            print "some error, the path doesnt exists"
    except Exception as e:
        return "err"


def runssss(asd):
    server_address = ('', 8001)
    httpd = HTTPServer(server_address, S)
    print 'Starting httpd...'
    httpd.serve_forever()


def clientthread(conn):
    # Sending message to connected client
    conn.sendall("ack\n")
    bho = 0
    # infinite loop so that function do not terminate and thread do not end.
    item = "ack\n"
    while True:
        # Receiving from client
        data = conn.recv(1024)

        if bho == 1:
            item = "ack\n"
            bho = 0
            b.put(data)

        item = q.get()
        bho = 1
        item = item.split(":")[1]
        print item

        reply = item + "\n"
        print data, " <-> ", reply
        # if bho == 0:
        #     bho = 1
        #     reply = "takeAll\n"

        # print data, reply
        # if not data:
        #     break

        conn.sendall(reply)
        if not q.empty():
            q.task_done()
    # came out of loop
    conn.close()


def createSocket():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    print 'Socket created'

    # Bind socket to local host and port
    try:
        s.bind(('', 8889))
    except socket.error as msg:
        print 'Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
        sys.exit()

    print 'Socket bind complete'

    # Start listening on socket
    s.listen(10)
    print 'Socket now listening'
    # now keep talking with the client
    while 1:
        # wait to accept a connection - blocking call
        conn, addr = s.accept()
        print 'Connected with ' + addr[0] + ':' + str(addr[1])

        # start new thread takes 1st argument as a function name to be run, second is the tuple of arguments to the function.

        Thread(target=clientthread, args=(conn,)).start()
    s.close()

if __name__ == "__main__":

    Thread(target=runssss, args=("",)).start()
    createSocket()

    print "ciao"
