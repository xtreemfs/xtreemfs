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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.mrc.replication;

import java.net.InetAddress;

import org.xtreemfs.interfaces.ReplicaSet;

/**
 * Interface for policies implementing replica selection policies.
 * 
 * @author stender
 */
public interface ReplicaSelectionPolicy {
    
    /**
     * Returns a list containing the same replicas as the input list, sorted by
     * the priority of replica selection in descending order, so that the first
     * replica in the list has the highest priority, the second the second
     * highest, etc.
     * 
     * @param replicas
     *            the original replica list
     * @param clientAddr
     *            client's IP address/hostname
     * @return the re-ordered list
     */
    public ReplicaSet getSortedReplicaList(ReplicaSet replicas, InetAddress clientAddr);
    
}
