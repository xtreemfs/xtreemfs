/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck (ZIB)
 */

package org.xtreemfs.osd;


public final class ErrorCodes {

    /**
     * the fileID is malformed or contains invalid characters
     */
    public static final int INVALID_FILEID = 1;

    /**
     * a header field is malformed or contains invalid values
     */
    public static final int INVALID_HEADER = 2;

    /**
     * the RPC request data is not valid
     */
    public static final int INVALID_RPC = 3;

    /**
     * HTTP or RPC method is not implemented
     */
    public static final int METHOD_NOT_IMPLEMENTED = 4;

    /**
     * the parameter count or type does not match for an RPC.
     */
    public static final int INVALID_PARAMS = 5;

    /**
     * this error code indicates that the server needs the full XLocation list
     * instead of the XLocation version number only.
     */
    public static final int NEED_FULL_XLOC = 10;

    /**
     * the XLocation list sent by the client is outdated and not accepted.
     */
    public static final int XLOC_OUTDATED = 11;

    /**
     * this server is not part of the XLocation list.
     */
    public static final int NOT_IN_XLOC = 12;

    /**
     * authentication failed.
     */
    public static final int AUTH_FAILED = 13;

    /**
     * checksum of an object turned out to be invalid
     */
    public static final int INVALID_CHECKSUM = 20;
    
    /**
     * the client is not the owner of the lease.
     */
    public static final int NOT_LEASE_OWNER = 30;
    
    /**
     * The lease has timed out (i.e. the timeout sent in the X-Lease-Timeout
     * header has passed).
     */
    public static final int LEASE_TIMED_OUT = 31;
    
    public static final int NO_REPLICA_AVAIL = 50;
    public static final int EPOCH_OUTDATED = 14;

    public static final int FILE_IS_READ_ONLY = 15;
}
