/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,
 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion
 and Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 *          Eugenio Cesario (CNR)
 */

package org.xtreemfs.osd;

public interface RPCTokens {

    /**
     * POST method name for getting the size of a file
     */
    public static final String fetchGlobalMaxToken     = "fetchGlobalMax";

    /**
     * POST method name for truncating a file with propagation
     */
    public static final String truncateTOKEN           = "truncate";

    /**
     * POST method name for truncating a file without propagation
     */
    public static final String truncateLocalTOKEN      = "truncateLocal";

    /**
     * POST method name for the deletion of locally stored objects of a file
     */
    public static final String deleteLocalTOKEN        = "deleteLocal";

    /**
     * POST method name for the retrieval of a commonly supported protocol
     * version
     */
    public static final String getProtocolVersionTOKEN = "getProtocolVersion";

    /**
     * POST method for shutting down the OSD
     */
    public static final String shutdownTOKEN           = ".shutdown";
    
    public static final String getstatsTOKEN           = "getStatistics";
    
    public static final String recordRqDurationTOKEN   = "recordRqDuration";

    /**
     * POST method for checking an object stored on the OSD
     */
    public static final String checkObjectTOKEN        = "checkObject";
    
    public static final String acquireLeaseTOKEN       = "acquireLease";
    
    public static final String returnLeaseTOKEN        = "returnLease";
    
    public static final String cleanUpTOKEN            = "cleanUp";

    // public static final String bufferstatsTOKEN = "/.sys.bufferstats";

    // public static final String queuestatsTOKEN = "/.sys.queuestats";
    
    public static final String readLocalTOKEN            = "localRead";
}
