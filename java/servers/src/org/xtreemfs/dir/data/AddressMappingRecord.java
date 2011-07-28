/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.data;

import java.io.IOException;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.AddressMapping;

/**
 *
 * @author bjko
 */
public class AddressMappingRecord {
    
    public static final byte CURRENT_VERSION = 1;
    private String uuid;
    private long version;

    private String protocol;

    private String address;

    private int port;

    private String match_network;

    private int ttl_s;

    private String uri;

    public AddressMappingRecord(AddressMapping m) {
        this.uuid = m.getUuid();
        this.version = m.getVersion();
        this.protocol = m.getProtocol();
        this.address = m.getAddress();
        this.port = m.getPort();
        this.match_network = m.getMatchNetwork();
        this.ttl_s = m.getTtlS();
        this.uri = m.getUri();
    }

    public AddressMappingRecord(ReusableBuffer rb) throws IOException {
        byte recVersion = rb.get();

        if (recVersion == 1) {
            uuid = rb.getString();
            version = rb.getLong();
            protocol = rb.getString();
            address = rb.getString();
            port = rb.getInt();
            match_network = rb.getString();
            ttl_s = rb.getInt();
            uri = rb.getString();
        } else {
            throw new IOException("don't know how to handle version "+recVersion);
        }
    }

    public int getSize() {
        final int INT_SIZE = Integer.SIZE/8;
        final int LONG_SIZE = Long.SIZE/8;
        final int BYTE_SIZE = Byte.SIZE/8;
        return INT_SIZE*7+LONG_SIZE+getUuid().length()+getProtocol().length()+getAddress().length()+getMatch_network().length()+
                getUri().length()+BYTE_SIZE;
    }

    public void serialize(ReusableBuffer rb) {

        rb.put(CURRENT_VERSION);
        rb.putString(getUuid());
        rb.putLong(getVersion());
        rb.putString(getProtocol());
        rb.putString(getAddress());
        rb.putInt(getPort());
        rb.putString(getMatch_network());
        rb.putInt(getTtl_s());
        rb.putString(getUri());

    }

    public AddressMapping getAddressMapping() {
        AddressMapping m = AddressMapping.newBuilder().setUuid(uuid).
                setVersion(version).setProtocol(protocol).setAddress(address).
                setPort(port).setMatchNetwork(match_network).setTtlS(ttl_s).
                setUri(uri).build();
        return m;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return the version
     */
    public long getVersion() {
        return version;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the match_network
     */
    public String getMatch_network() {
        return match_network;
    }

    /**
     * @return the ttl_s
     */
    public int getTtl_s() {
        return ttl_s;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }



}
