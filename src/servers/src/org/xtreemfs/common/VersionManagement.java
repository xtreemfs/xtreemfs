/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 */

package org.xtreemfs.common;


/**
 * This class is meant to maintain version numbers for different components used
 * in XtreemFS, in order to be able to detect possible incompatibilities between
 * different versions.
 *
 * When a new version of the protocol, database, etc. has been implemented, the
 * corresponding version number should be replaced. XtreemFS will rely on this
 * class to find out what the current version numbers are.
 *
 */
public class VersionManagement {

    private static final long   mrcDataVersion            = 8;

    private static final long   osdDataVersion            = 1;

    public static long getMrcDataVersion() {
        return mrcDataVersion;
    }

    public static long getOsdDataVersion() {
        return osdDataVersion;
    }

}
