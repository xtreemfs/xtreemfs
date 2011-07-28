This is a web based admin console for XtreemFS (release 1.2).
It requires Java 1.6+ and a Tomcat 5.x+.

The console includes Google's GWT which is copyright by Google and
licensed under the apache license 2.0 (see 
http://code.google.com/webtoolkit/terms.html for details).

XtreemFS is copyright by ZIB, NEC, BSC and CNR and licensed
under GPLv2. For details see http://www.xtreemfs.org

This console is copyright by Bjoern Kolbeck, ZIB.


INSTALLATION
------------
Install with Tomcat's manager.

CONFIGURATION
-------------
You have to edit 'config.properties' and 'users.properties'
which are both located in the root directory of the webapp.

In 'users.properties' add a line with <username>=<password>
for each user.

In 'config.properties' you should add a list of directory
services you would like to access with this console. Usually,
this is only a single DIR, e.g.
dir_servers = oncrpc://myDIRmachine:32638

If you use SSL or GridSSL you have to use oncrpcs or oncrpcg instead.
In addition, you have to configure which client certificate and
which trusted certificates to use:
ssl.service_creds = /etc/xos/xtreemfs/client.p12
ssl.service_creds.pw = passphrase
ssl.service_creds.container = pkcs12

ssl.trusted_certs = /etc/xos/xtreemfs/trusted.jks
ssl.trusted_certs.pw = passphrase
ssl.trusted_certs.container = jks

If you want to use google maps, you have to get a
key from Google for your site and store it in
google_maps_key = ABCDEFG...
You don't need a key if you run the console on your local machine
only.
