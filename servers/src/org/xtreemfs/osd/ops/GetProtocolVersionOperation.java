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

package org.xtreemfs.osd.ops;

import java.util.ArrayList;
import java.util.List;

import org.xtreemfs.common.VersionManagement;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.ErrorRecord.ErrorClass;

public final class GetProtocolVersionOperation extends Operation {

    public GetProtocolVersionOperation(OSDRequestDispatcher master) {
        super(master);
    }

    public void startRequest(OSDRequest rq) {
        final long ver = (Long) rq.getAttachment();
        rq.setData(ReusableBuffer.wrap(Long.toString(ver).getBytes()),
            DATA_TYPE.JSON);
        finishRequest(rq);
    }

    public ErrorRecord parseRPCBody(OSDRequest rq, List<Object> arguments) {

        List<Long> versions = new ArrayList<Long>(arguments.size());
        for (Object arg : arguments) {
            if (arg instanceof Long)
                versions.add((Long) arg);
            else
                return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                    "invalid version number: " + arg);
        }

        final long result = VersionManagement.getMatchingProtVers(versions);

        if (result == -1)
            return new ErrorRecord(ErrorClass.USER_EXCEPTION,
                "No matching protocol version found. Server supports "
                    + VersionManagement.getSupportedProtVersAsString());

        rq.setAttachment(result);
        return null;
    }

}
