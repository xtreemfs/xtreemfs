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
import java.util.HashMap;
import java.util.Map;

import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.SliceID;

public class PartitioningManager {

    private final SliceManager sliceMan;

    private final Map<Long, PartitioningPolicy> policies;

    public PartitioningManager(SliceManager sliceMan) {

        this.sliceMan = sliceMan;

        policies = new HashMap<Long, PartitioningPolicy>();
        policies.put(DefaultPartitioningPolicy.POLICY_ID,
                new DefaultPartitioningPolicy(sliceMan));
    }

    public Collection<SliceInfo> getInitialSlices(String volumeId)
            throws BackendException {
        return getPolicy(volumeId).getInitialSlices(volumeId);
    }

    public SliceID getSlice(String volumeId, String path)
            throws BackendException {
        return getPolicy(volumeId).getSlice(volumeId, path);
    }
    
    public SliceID getSlice(String volumeId, long fileId)
            throws BackendException {
        return getPolicy(volumeId).getSlice(volumeId, fileId);
    }

    protected PartitioningPolicy getPolicy(String volumeId)
            throws BackendException {

        try {
            long policyId = sliceMan.getVolumeById(volumeId)
                    .getPartitioningPolicyId();

            PartitioningPolicy policy = policies.get(policyId);
            if (policy == null)
                throw new BackendException(
                        "unknown partitioning policy for volume " + volumeId
                                + ": " + policyId);

            return policy;

        } catch (UserException exc) {
            throw new BackendException(exc);
        }
    }

}
