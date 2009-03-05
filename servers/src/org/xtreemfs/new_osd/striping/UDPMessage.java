/*  Copyright (c) 2008,2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin,.

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

package org.xtreemfs.new_osd.striping;

import java.net.InetSocketAddress;
import org.xtreemfs.common.buffer.ReusableBuffer;

public class UDPMessage {

    /**
     * @return the msgType
     */
    public Type getMsgType() {
        return msgType;
    }

    /**
     * @return the address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * @return the payload
     */
    public ReusableBuffer getPayload() {
        return payload;
    }

    public static enum Type {
        GMAX,
        MPXL
    };

    private final Type msgType;

    private final InetSocketAddress address;

    private final ReusableBuffer    payload;

    public UDPMessage(Type msgType, InetSocketAddress address, ReusableBuffer payload) {
        this.msgType = msgType;
        this.address = address;
        this.payload = payload;
        payload.position(0);
        payload.put((byte)msgType.ordinal());
        payload.position(0);
    }

    public UDPMessage(InetSocketAddress address, ReusableBuffer payload) {
        this.address = address;
        this.payload = payload;
        payload.position(0);
        int tmp = payload.get();
        msgType = Type.values()[tmp];
    }

}