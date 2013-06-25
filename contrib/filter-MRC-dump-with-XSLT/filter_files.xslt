<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:date="http://exslt.org/dates-and-times"
                extension-element-prefixes="date">
                
<!--
Copyright (c) 2013 by Michael Berlin, Zuse Institute Berlin

Licensed under the BSD License, see LICENSE file for details.

This file transforms a MRC database dump (in XML format) into
a list of files. The output format is as follows:

  volume name/path on volume|creation time|file size|file's owner name

The current version lists only files which are placed on an OSD
with the UUID 'zib.mosgrid.osd15' (see line 34).

You can use the 'xsltproc' tool to apply this transformation to a XML dump.

Example: xsltproc -o filtered_files_output.txt filter_files.xslt /tmp/dump.xml
-->

<xsl:output omit-xml-declaration="yes"/>

<!--Strip off white space from all elements. We take care of the format on our own.-->
<xsl:strip-space elements="*"/>

<!--For each volume, process its "file" elements.-->
<xsl:template match="volume">
  <xsl:apply-templates select="//file"/>
</xsl:template>

<xsl:template match="file[xlocList/xloc/osd/@location='zib.mosgrid.osd15']">
  <!--Traverse the path of the <file> element and output the 'name' attribute of
  each element to display the file system path.
  The first entry is the name of the volume.-->
  <xsl:for-each select="ancestor-or-self::*/@name">
    <!--We ignore the <volume> element because its name is repeated as <dir> element below.-->
    <xsl:if test="local-name(..) != 'volume'">
    
      <!--Output path element.-->
      <xsl:value-of select="."/>
      
      <xsl:if test="position() != last()">
        <!--Display separator.-->
        <xsl:text>/</xsl:text>
      </xsl:if>
      
    </xsl:if>
  </xsl:for-each>
  
  <!--Creation time.-->
  <xsl:text>|</xsl:text>
  <xsl:value-of select="date:add('1970-01-01T00:00:00Z', date:duration(@ctime))"/>

  <!--File size.-->
  <xsl:text>|</xsl:text>
  <xsl:value-of select="@size"/>

  <!--Owner.-->
  <xsl:text>|</xsl:text>
  <xsl:value-of select="@uid"/>
  
  <!--New line.-->
  <xsl:text>&#xa;</xsl:text>
</xsl:template>

</xsl:stylesheet>