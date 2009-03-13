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
 * AUTHORS: Minor Gordon (NEC), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;

public class ONCRPCRequestHeader implements Serializable {

    public ONCRPCRequestHeader() {
        xid = prog = vers = proc = 0;
    }

    public ONCRPCRequestHeader(int xid, int prog, int vers, int proc) {
        this.xid = xid;
        this.prog = prog;
        this.vers = vers;
        this.proc = proc;
    }

    public int getXID() {
        return xid;
    }

    public int getInterfaceNumber() {
        return prog - 0x20000000;
    }

    public int getInterfaceVersion() {
        return vers;
    }

    public int getOperationNumber() {
        return proc;
    }

    public int getMessageType() {
        return msg_type;
    }

    public int getRpcVersion() {
        return rpcvers;
    }

    public String toString() {
        return "ONCRPCRequestHeader( " + Integer.toString(proc) + " )";
    }

    // Serializable    
    public String getTypeName() { return "xtreemfs::interfaces::ONCRPCRequestHeader"; }    
    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt(xid);
        writer.putInt(0); // CALL
        writer.putInt(2); // RPC version 2
        writer.putInt(prog);
        writer.putInt(vers);
        writer.putInt(proc);
        writer.putInt(0); // cred_auth_flavor
        writer.putInt(0); // cred auth opaque data
        writer.putInt(0); // verf_auth_flavor
        writer.putInt(0); // verf auth opaque data
    }

    public void deserialize(ReusableBuffer buf) {
        xid = buf.getInt();
//        System.out.println( "XID " + Integer.toString( xid ) );
        msg_type = buf.getInt();
        rpcvers = buf.getInt();
//        System.out.println( "RPC version " + Integer.toString( rpcvers ) );
        prog = buf.getInt();
        //       System.out.println( "Prog " + Integer.toString( prog ) );
        vers = buf.getInt();
//        System.out.println( "Vers " + Integer.toString( vers ) );        
        proc = buf.getInt();
//        System.out.println( "proc " + Integer.toString( proc ) );        
        buf.getInt(); // cred_auth_flavor
        buf.getInt(); // cred auth opaque data
        buf.getInt(); // verf_auth_flavor
        buf.getInt(); // verf auth opaque data
    }

    public int calculateSize() {
        return 10 * Integer.SIZE/8;
    }
    private int xid;

    private int prog;

    private int vers;

    private int proc;

    private int rpcvers;

    private int msg_type;

}
