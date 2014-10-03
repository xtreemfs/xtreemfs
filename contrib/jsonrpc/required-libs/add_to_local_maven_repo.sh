#!/bin/sh

echo "run this script to add the needed XtreemFS.jar and Foundation.jar files to your local maven repository"
echo
echo "hit enter to continue"
read bogus

# these files were built from the xtreemfs sources available at http://code.google.com/p/xtreemfs/
mvn install:install-file -Dfile=XtreemFS.jar -DgroupId=org.xtreemfs -DartifactId=xtreemfs -Dversion=1.4-4.1 -Dpackaging=jar
mvn install:install-file -Dfile=Foundation.jar -DgroupId=org.xtreemfs.foundation -DartifactId=xtreemfs-foundation -Dversion=1.4-4.1 -Dpackaging=jar
mvn install:install-file -Dfile=Flease.jar -DgroupId=org.xtreemfs -DartifactId=xtreemfs-flease -Dversion=1.4-4.1 -Dpackaging=jar

#mvn install:install-file -Dfile=jsch-0.1.50.jar -DgroupId=com.jcraft.jsch -DartifactId=jsch -Dversion=0.1.50 -Dpackaging=jar

mvn install:install-file -Dfile=jsonrpc2-base-1.27.jar -DgroupId=com.thetransactioncompany -DartifactId=jsonrpc-base -Dversion=1.27 -Dpackaging=jar
mvn install:install-file -Dfile=jsonrpc2-server-1.7.jar -DgroupId=com.thetransactioncompany.server -DartifactId=jsonrpc-server -Dversion=1.7 -Dpackaging=jar
mvn install:install-file -Dfile=jsonrpc2-client-1.8.jar -DgroupId=com.thetransactioncompany.client -DartifactId=jsonrpc-client -Dversion=1.8 -Dpackaging=jar

echo "if the files were successfully added, you can now run \"mvn package\" from the parent directory (which also contains the pom.xml)"
echo "afterwards you can deploy the resulting file \"target/ROOT.war\" file in your servlet-container using \"mvn tomcat:redeploy\" (e.g. Jetty or Tomcat)"
