/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.foundation.pinky;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Simple class to encapsulate HTTP headers.
 * This class is <b>not thread safe</b>
 * @author bjko
 */
public class HTTPHeaders implements Iterable<HTTPHeaders.HeaderEntry> {

    /** Number of slots for headers to allocate initially
     */
    private static final int INIT_NUM_HDRS = 10;

    /** HTTP header for content (body) type
     */
    public static final String HDR_CONTENT_TYPE = "Content-Type";

    /** HTTP header for content (body) length
     */
    public static final String HDR_CONTENT_LENGTH = "Content-Length";

    /** HTTP header for content range
     */
    public static final String HDR_CONTENT_RANGE = "Content-Range";

    /** HTTP header for authorization
     */
    public static final String HDR_AUTHORIZATION = "Authorization";
    
    /** HTTP header to request authentication from client
     */
    public static final String HDR_WWWAUTH = "WWW-Authenticate";

    /** (non standard) HTTP header for sending replica locations
     */
    public static final String HDR_XLOCATIONS = "X-Locations";

    /** (non standard) HTTP header containing the capability issued by the MRC
     */
    public static final String HDR_XCAPABILITY = "X-Capability";

    /** (non standard) HTTP header for communicating file size updates to the MRC
     */
    public static final String HDR_XNEWFILESIZE = "X-New-File-Size";

    /** (non standard) HTTP header containing the object number
     */
    public static final String HDR_XOBJECTNUMBER = "X-Object-Number";

    public static final String HDR_XVERSIONNUMBER = "X-Version-Number";

    /** (non standard) HTTP header for sending the target location for a file
     * replication request
     */
    public static final String HDR_XTARGETLOCATION = "X-Target-Location";

    public static final String HDR_XINVALIDCHECKSUM = "X-Invalid-Checksum";

    /** (non standard) HTTP header indicating what locations will be excluded
     *  @deprecated
     */
    public static final String HDR_XEXCLUDEDLOCATION = "X-Excluded-Location";

    /** (non standard) HTTP header indicating a fileID
     */
    public static final String HDR_XFILEID = "X-FileID";
    
    public static final String HDR_XLEASETO = "X-Lease-Timeout";
    
    public static final String HDR_XREQUESTID = "X-Request-Id";


    /** HTTP header for location
     */
    public static final String HDR_LOCATION = "Location";

    /** list of headers
     */
    private ArrayList<HeaderEntry> hdrs;

    /**
     * Creates a new instance of HTTPHeaders
     */
    public HTTPHeaders() {
        hdrs = new ArrayList(INIT_NUM_HDRS);
    }

    /** adds an entry to the header list
     */
    public void addHeader(String name, String value) {
        hdrs.add(new HeaderEntry(name,value));
    }

    /** adds an entry to the header list
     */
    public void addHeader(String name, int value) {
        hdrs.add(new HeaderEntry(name,Integer.toString(value)));
    }

    /** parses 'headerName: headerValue' strings.
     */
    public void addHeader(String line) {
        int colonPos = line.indexOf(':');
        if (colonPos > 0) {

            String name = line.substring(0,colonPos).trim();
            String value = line.substring(colonPos+1).trim();
            hdrs.add(new HeaderEntry(name,value));
        }
    }

    /** sets a header value or adds that header if it was not in the list before
     */
    public void setHeader(String name, String value) {
        for (HeaderEntry he : hdrs) {
            if (he.name.equalsIgnoreCase(name)) {
               he.value = value;
               return;
            }
        }
        hdrs.add(new HeaderEntry(name,value));
    }

    /** It gets a header's value
     *  @param name Header's identifier
     *  @return The value of the header or null if the header doesn't exist in the object.
     */
    public String getHeader(String name) {
        for (HeaderEntry he : hdrs) {
            if (he.name.equalsIgnoreCase(name)) {
                return he.value;
            }
        }
        return null;
    }

    /** converts all header in the list into a
     *  HTTP conform header string
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.append(sb);
        return sb.toString();
    }
    
    public void append(StringBuilder sb) {
        for (HeaderEntry he : hdrs) {
            sb.append(he.name);
            sb.append(": ");
            sb.append(he.value);
            sb.append(HTTPUtils.CRLF);
        }
    }

    public void parse(String headers) {
        int nextNL = headers.indexOf("\n");
        int cPos = 0;

        while (nextNL != -1) {

            String line = headers.substring(cPos, nextNL);
            cPos = nextNL + 1;
            nextNL = headers.indexOf("\n", cPos);

            this.addHeader(line);
        }
    }
    
    public void copyFrom(HTTPHeaders other) {
        for (HeaderEntry hdr : other.hdrs) {
            this.setHeader(hdr.name, hdr.value);
        }
    }
    
    public int getSizeInBytes() {
        int numBytes = 0;
        for (HeaderEntry he : hdrs) {
            numBytes += he.name.length()+2+he.value.length()+HTTPUtils.CRLF.length();
        }
        return numBytes;
    }

    /** simple class to store header name and value
     */
    public static final class HeaderEntry {
        public String name;
        public String value;
        public HeaderEntry(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public Iterator<HeaderEntry> iterator() {
        return new Iterator<HeaderEntry>() {

            int position = 0;

            public boolean hasNext() {
                return position < hdrs.size();
            }

            public HeaderEntry next() {
                return hdrs.get(position++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }

}
