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

package org.xtreemfs.new_mrc.operations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.utils.MessageUtils;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class GetLocalVolumesOperation extends MRCOperation {
    
    static class Args {
        public List<Long> proposedVersions;
    }
    
    public static final String RPC_NAME = "getLocalVolumes";
    
    public GetLocalVolumesOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return false;
    }
    
    @Override
    public boolean isAuthRequired() {
        return false;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            List<VolumeInfo> volumes = master.getVolumeManager().getVolumes();
            
            Map<String, String> map = new HashMap<String, String>();
            for (VolumeInfo data : volumes)
                map.put(data.getId(), data.getName());
            
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(map).getBytes()));
            finishRequest(rq);
            
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
}
