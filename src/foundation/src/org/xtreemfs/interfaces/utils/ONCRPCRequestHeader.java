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

package org.xtreemfs.interfaces.utils;

import yidl.runtime.Marshaller;
import yidl.runtime.Unmarshaller;

public class ONCRPCRequestHeader implements yidl.runtime.Object {
    private static final long serialVersionUID = -5006903752501953411L;

    public ONCRPCRequestHeader(yidl.runtime.Object cred) {
        xid = prog = vers = proc = 0;
        user_credentials = cred;
    }

    public ONCRPCRequestHeader(int xid, int prog, int vers, int proc, yidl.runtime.Object cred) {
        this.xid = xid;
        this.prog = prog;
        this.vers = vers;
        this.proc = proc;
        this.user_credentials = cred;
    }

    public ONCRPCRequestHeader(int xid, int prog, int vers, int proc) {
        this(xid,prog,vers,proc,null);
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

    public int getProcedure() {
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
    public int getTag() { return getProcedure(); }
    public String getTypeName() { return "xtreemfs::interfaces::ONCRPCRequestHeader"; }    
    
    public void marshal(Marshaller writer) {
        writer.writeInt32(null,xid);
        writer.writeInt32(null,0); // CALL
        writer.writeInt32(null,2); // RPC version 2
        writer.writeInt32(null,prog);
        writer.writeInt32(null,vers);
        writer.writeInt32(null,proc);
        if (user_credentials != null) {
            writer.writeInt32(null,user_credentials.getTag());
            final int dataLength = user_credentials.getXDRSize();
            writer.writeInt32(null,dataLength);
            user_credentials.marshal(writer);
        } else {
            writer.writeInt32(null,0); // cred_auth_flavor
            writer.writeInt32(null,0); // cred auth opaque data
        }

        
        writer.writeInt32(null,0); // verf_auth_flavor
        writer.writeInt32(null,0); // verf auth opaque data
    }

    public void unmarshal(Unmarshaller um) {
        xid = um.readInt32(null);
//        System.out.println( "XID " + Integer.toString( xid ) );
        msg_type = um.readInt32(null);
        rpcvers = um.readInt32(null);
//        System.out.println( "RPC version " + Integer.toString( rpcvers ) );
        prog = um.readInt32(null);
        //       System.out.println( "Prog " + Integer.toString( prog ) );
        vers = um.readInt32(null);
//        System.out.println( "Vers " + Integer.toString( vers ) );        
        proc = um.readInt32(null);
//        System.out.println( "proc " + Integer.toString( proc ) );        
        auth_flavor = um.readInt32(null); // cred_auth_flavor
        int auth_size = um.readInt32(null);
        if (auth_flavor != 0) {
            if (user_credentials != null) {
                if (auth_flavor != user_credentials.getTag())
                    throw new IllegalArgumentException("user_credentials has invalid type: "+user_credentials.getTag());
                user_credentials.unmarshal(um);
            } else {
                user_credentials = null;
                //skip
                for (int i = 0; i < auth_size; i++)
                    um.readInt8(null);
            }
        } else {
            user_credentials = null;
        }
        um.readInt32(null); // verf_auth_flavor
        um.readInt32(null); // verf auth opaque data
    }

    public int getXDRSize() {
        final int authSize = (getUser_credentials() != null) ? getUser_credentials().getXDRSize() : 0;
        return 10 * Integer.SIZE/8 + authSize;
    }
    private int xid;

    private int prog;

    private int vers;

    private int proc;

    private int rpcvers;

    private int msg_type;

    private int auth_flavor;

    private yidl.runtime.Object user_credentials;

    /**
     * @return the auth_flavor
     */
    public int getAuth_flavor() {
        return auth_flavor;
    }

    /**
     * @return the user_credentials
     */
    public yidl.runtime.Object getUser_credentials() {
        return user_credentials;
    }


}
