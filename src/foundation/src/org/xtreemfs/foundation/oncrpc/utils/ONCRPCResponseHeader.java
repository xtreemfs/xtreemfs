/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Minor Gordon (NEC), Bjoern Kolbeck (ZIB)
 */

package org.xtreemfs.foundation.oncrpc.utils;

import yidl.runtime.Marshaller;
import yidl.runtime.Unmarshaller;

public class ONCRPCResponseHeader implements yidl.runtime.Object {
    private static final long serialVersionUID = -7434320317448962162L;

    public static final int REPLY_STAT_MSG_ACCEPTED = 0;

    public static final int REPLY_STAT_MSG_DENIED = 1;

    public static final int ACCEPT_STAT_SUCCESS = 0;

    public static final int ACCEPT_STAT_PROG_UNAVAIL = 1;

    public static final int ACCEPT_STAT_PROG_MISMATCH = 2;

    public static final int ACCEPT_STAT_PROC_UNAVAIL = 3;

    public static final int ACCEPT_STAT_GARBAGE_ARGS = 4;

    public static final int ACCEPT_STAT_SYSTEM_ERR = 5;

    public ONCRPCResponseHeader() {
        this(0);
    }

    public ONCRPCResponseHeader(int xid) {
        this(xid, REPLY_STAT_MSG_ACCEPTED);
    }

    public ONCRPCResponseHeader(int xid, int reply_stat) {
        this(xid, reply_stat, ACCEPT_STAT_SUCCESS);
    }

    public ONCRPCResponseHeader(int xid, int reply_stat, int accept_stat) {
        this.xid = xid;
        this.reply_stat = reply_stat;
        this.accept_stat = accept_stat;
    }

    public int getXID() {
        return xid;
    }

    public int getAcceptStat() {
        return accept_stat;
    }

    public int getReplyStat() {
        return reply_stat;
    }    
    
    // Serializable    
    public String getTypeName() { return "xtreemfs::interfaces::ONCRPCResponseHeader"; }
    
    public void marshal(Marshaller writer) {
        writer.writeInt32(null,xid);
        //message type is REPLY = 1
        writer.writeInt32(null,1);
        writer.writeInt32(null,reply_stat);
        //auth information, unused for xtreemfs
        writer.writeInt32(null,0); // AUTH_NONE
        writer.writeInt32(null,0); // zero opaque auth data
        writer.writeInt32(null,accept_stat);
    }

    public void unmarshal(Unmarshaller buf) {
        xid = buf.readInt32(null);
        int msgType = buf.readInt32(null);
        if (msgType != 1)
            throw new IllegalArgumentException("message type must be ANSWER, but is "+msgType);
        reply_stat = buf.readInt32(null);
        if (reply_stat != REPLY_STAT_MSG_ACCEPTED)
            throw new IllegalArgumentException("message type must be ANSWER, but is "+msgType);

        final int authType = buf.readInt32(null);
        assert authType == 0;

        final int opaqueAuthData = buf.readInt32(null);
        assert opaqueAuthData == 0;
        
        accept_stat = buf.readInt32(null);
    }

    public int getXDRSize() {
        return 6 * Integer.SIZE / 8;
    }
    private int xid;

    private int reply_stat;

    private int accept_stat;

    @Override
    public int getTag() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
