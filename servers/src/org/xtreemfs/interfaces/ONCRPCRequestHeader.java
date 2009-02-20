package org.xtreemfs.interfaces;

import java.nio.ByteBuffer;


public class ONCRPCRequestHeader extends ONCRPCRecordFragmentHeader
{    
    public ONCRPCRequestHeader()
    {
        xid = prog = vers = proc = 0;        
    }
    
    public ONCRPCRequestHeader( int xid, int prog, int vers, int proc )
    {
        this.xid = xid;
        this.prog = prog;
        this.vers = vers;
        this.proc = proc;
    }
    
    public int getXID() { return xid; }
    public int getInterfaceNumber() { return prog - 20000000; }
    public int getInterfaceVersion() { return vers; }
    public int getOperationNumber() { return proc; }

    public String toString()
    {
        return "ONCRPCRequestHeader( " + Integer.toString( proc ) + " with record fragment size " + Integer.toString( getRecordFragmentLength() ) + " )";
    }
        
    // Serializable    
    public void serialize( ByteBuffer buf )
    {
        super.serialize( buf );
        buf.putInt( xid );
        buf.putInt( 0 ); // CALL
        buf.putInt( 2 ); // RPC version 2
        buf.putInt( prog );
        buf.putInt( vers );
        buf.putInt( proc );
        buf.putInt( 0 ); // cred_auth_flavor
        buf.putInt( 0 ); // verf_auth_flavor
    }
    
    public void deserialize( ByteBuffer buf )
    {        
        super.deserialize( buf );
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

    public int getSize()
    {
        return super.getSize() + 4 * 4;
    }

    
    private int xid;
    private int prog;
    private int vers;
    private int proc;    
}
