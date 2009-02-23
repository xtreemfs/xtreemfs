package org.xtreemfs.interfaces.utils;

import java.nio.ByteBuffer;
import java.util.List;
import org.xtreemfs.common.buffer.BufferPool;
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
        return prog - 20000000;
    }

    public int getInterfaceVersion() {
        return vers;
    }

    public int getOperationNumber() {
        return proc;
    }

    public String toString() {
        return "ONCRPCRequestHeader( " + Integer.toString(proc) + " )";
    }

    // Serializable    
    public void serialize(ONCRPCBufferWriter writer) {
        writer.putInt(xid);
        writer.putInt(0); // CALL
        writer.putInt(2); // RPC version 2
        writer.putInt(prog);
        writer.putInt(vers);
        writer.putInt(proc);
        writer.putInt(0); // cred_auth_flavor
        writer.putInt(0); // verf_auth_flavor
    }

    public void deserialize(ReusableBuffer buf) {
        xid = buf.getInt();
//        System.out.println( "XID " + Integer.toString( xid ) );
        int msg_type = buf.getInt();
        assert msg_type == 0; // CALL    
        int rpcvers = buf.getInt();
//        System.out.println( "RPC version " + Integer.toString( rpcvers ) );
        assert rpcvers == 2;
        prog = buf.getInt();
        //       System.out.println( "Prog " + Integer.toString( prog ) );
        vers = buf.getInt();
//        System.out.println( "Vers " + Integer.toString( vers ) );        
        proc = buf.getInt();
//        System.out.println( "proc " + Integer.toString( proc ) );        
        buf.getInt(); // cred_auth_flavor
        buf.getInt(); // verf_auth_flavor
    }

    public int getSize() {
        return 8 * Integer.SIZE/8;
    }
    private int xid;

    private int prog;

    private int vers;

    private int proc;

}
