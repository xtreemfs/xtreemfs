XtreemFS Hadoop Client
======================

In your `$HOME/.m2/settings.xml` add:
```XML
<settings>
  <profiles>

    <!-- more profiles -->
  
    <profile>
      <id>xtreemfs-hadoop-client-dev</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>http://repo.maven.apache.org/maven2</url>
        </repository>

        <repository>
          <id>xtreemfs-xtreemfs</id>
          <url>https://xtreemfs.github.io/xtreemfs/maven</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>

        <repository>
          <id>xtreemfs-babudb</id>
          <url>https://xtreemfs.github.io/babudb/maven</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>

        <repository>
          <id>xtreemfs-opendmk</id>
          <url>https://xtreemfs.github.io/opendmk</url>
          <snapshots>
            <enabled>true</enabled>
          </snapshots>
        </repository>
      </repositories>
    </profile>
  
    <!-- more profiles -->
  
  </profiles>
</settings>
```

In your `pom.xml` add:
```XML
<project>

  <!-- more project configuration -->

  <dependencies>
    <dependency>
      <groupId>org.xtreemfs.xtreemfs</groupId>
      <artifactId>xtreemfs-hadoop-client</artifactId>
      <version>1.5.1-SNAPSHOT</version>
      <!-- The shaded version bundles:                                                                   -->
      <!-- - com.google.protobuf:protobuf-java                                                           -->
      <!-- - org.xtreemfs.xtreemfs:xtreemfs-foundation                                                   -->
      <!-- - org.xtreemfs.xtreemfs:xtreemfs-pbrpcgen/org.xtreemfs.foundation.pbrpc.generatedinterfaces.* -->
      <!-- - org.xtreemfs.xtreemfs:xtreemfs-servers                                                      -->
      <!-- and includes applicable licenses.                                                             -->
      <!-- <classifier>shaded</classifier>                                                               -->
    </dependency>
  </dependencies>

  <!-- more project configuration -->

</project>
```

And build your project like so:
```Bash
  mvn install -Pxtreemfs-hadoop-dev
```
