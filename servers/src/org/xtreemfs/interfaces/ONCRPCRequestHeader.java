package org.xtreemfs.interfaces;

import java.nio.ByteBuffer;
import java.util.List;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.buffer.ReusableBuffer;

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
    public void serialize(List<ReusableBuffer> buffers) {
        ReusableBuffer buf = BufferPool.allocate(this.getSize());
        buf.putInt(xid);
        buf.putInt(0); // CALL
        buf.putInt(2); // RPC version 2
        buf.putInt(prog);
        buf.putInt(vers);
        buf.putInt(proc);
        buf.putInt(0); // cred_auth_flavor
        buf.putInt(0); // verf_auth_flavor
        buf.position(0);
        buffers.add(buf);
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
