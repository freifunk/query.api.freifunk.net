# coding: utf-8

import json 
import urllib2
import base64

fp = urllib2.urlopen("http://freifunk.net/map/ffSummarizedDir.json")

url = 'http://localhost:8080/core/topic'
username = 'admin'
password = ''



def post(string):
    data = json.dumps(dict(type_uri='freifunk.community.name',value=string))
    user = 'admin'
    password = ''
    base64string = base64.encodestring('%s:%s' % (user, password)).replace('\n', '')
    request = urllib2.Request(url,data)
    request.get_method = lambda: 'POST'
    request.add_header("Content-Type", "application/json")
    request.add_header("Authorization", "Basic %s" % base64string) 
    print urllib2.urlopen(request).read()



data = json.load(fp)

for i in data.itervalues():
    #print i['name']+' '+i['api']
    post(i['name'])


