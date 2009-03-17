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

package org.xtreemfs.osd.striping;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

/**
 *
 * @author bjko
 */
public class GMAXMessage {


    private final String fileId;

    private final long   truncateEpoch;

    private final long   lastObject;

    private final static int MAX_FILEID = 128;

    public GMAXMessage(String fileId, long truncateEpoch, long lastObject) {
        this.fileId = fileId;
        this.truncateEpoch = truncateEpoch;
        this.lastObject = lastObject;
    }

    public GMAXMessage(UDPMessage msg) {
        final ReusableBuffer data = msg.getPayload();
        data.position(1);
        int strlen = data.getInt();
        if (strlen > MAX_FILEID) {
            throw new IllegalArgumentException("invalid UDP message, fileID too long.");
        }
        byte[] bytes = new byte[strlen];
        data.get(bytes);
        fileId = new String(bytes);
        truncateEpoch = data.getLong();
        lastObject = data.getLong();
        BufferPool.free(data);
    }

    public UDPMessage getMessage(InetSocketAddress receiver) {
        byte[] bytes = getFileId().getBytes();
        ReusableBuffer data = BufferPool.allocate(Long.SIZE/8*2+Integer.SIZE/8+bytes.length+1);
        //leave one byte for the message type set by UDPMessage
        data.position(1);

        data.putInt(bytes.length);
        data.put(bytes);
        data.putLong(getTruncateEpoch());
        data.putLong(getLastObject());
        return new UDPMessage(UDPMessage.Type.GMAX, receiver, data);

    }

    /**
     * @return the fileId
     */
    public String getFileId() {
        return fileId;
    }

    /**
     * @return the truncateEpoch
     */
    public long getTruncateEpoch() {
        return truncateEpoch;
    }

    /**
     * @return the lastObject
     */
    public long getLastObject() {
        return lastObject;
    }
}
