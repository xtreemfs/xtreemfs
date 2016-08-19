XtreemFS Java Components
========================

In your `$HOME/.m2/settings.xml` add:
```XML
<settings>
  <profiles>

    <!-- more profiles -->
  
    <profile>
      <id>xtreemfs-dev</id>
      <repositories>
        <repository>
          <id>central</id>
          <url>http://repo.maven.apache.org/maven2</url>
        </repository>

        <repository>
          <id>babudb-repository</id>
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
      <artifactId>xtreemfs-flease</artifactId>
      <version>1.6.0</version>
    </dependency>

    <dependency>
      <groupId>org.xtreemfs.xtreemfs</groupId>
      <artifactId>xtreemfs-foundation</artifactId>
      <version>1.6.0</version>
    </dependency>

    <dependency>
      <groupId>org.xtreemfs.xtreemfs</groupId>
      <artifactId>xtreemfs-pbrpcgen</artifactId>
      <version>1.6.0</version>
    </dependency>

    <dependency>
      <groupId>org.xtreemfs.xtreemfs</groupId>
      <artifactId>xtreemfs-servers</artifactId>
      <version>1.6.0</version>
      <!-- The shaded version bundles:                                                                   -->
      <!-- - com.google.protobuf:protobuf-java                                                           -->
      <!-- - commons-codec:commons-codec                                                                 -->
      <!-- - org.xtreemfs.babudb:babudb-core                                                             -->
      <!-- - org.xtreemfs.opendmk:jdmkrt                                                                 -->
      <!-- - org.xtreemfs.xtreemfs:xtreemfs-flease                                                       -->
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
  mvn install -Pxtreemfs-dev
```
