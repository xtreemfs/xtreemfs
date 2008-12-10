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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.slices;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.SliceID;

/**
 * A simple partitioning policy that only maintains a single slice per volume.
 * 
 * @author stender
 * 
 */
public class DefaultPartitioningPolicy implements PartitioningPolicy {

    public static final long POLICY_ID = 1;

    private final SliceManager sliceMan;

    public DefaultPartitioningPolicy(SliceManager sliceMan) {
        this.sliceMan = sliceMan;
    }

    public Collection<SliceInfo> getInitialSlices(String volumeId)
            throws BackendException {

        try {
            Set<SliceInfo> slices = new HashSet<SliceInfo>();
            slices.add(new SliceInfo(new SliceID(volumeId, 1), null));
            return slices;
        } catch (Exception exc) {
            throw new BackendException(exc);
        }
    }

    public SliceID getSlice(String volumeId, String path)
            throws BackendException {
        try {
            return sliceMan.getVolumeById(volumeId).getSlices().iterator()
                    .next().sliceID;
        } catch (UserException exc) {
            throw new BackendException(exc);
        }
    }

    public SliceID getSlice(String volumeId, long fileId)
            throws BackendException {
        try {
            return sliceMan.getVolumeById(volumeId).getSlices().iterator()
                    .next().sliceID;
        } catch (UserException exc) {
            throw new BackendException(exc);
        }
    }
}
