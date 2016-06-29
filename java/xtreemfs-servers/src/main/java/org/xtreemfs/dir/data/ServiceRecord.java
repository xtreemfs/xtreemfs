/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceDataMap;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.ServiceType;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

/**
 *
 * @author bjko
 */
public class ServiceRecord {

    public static final byte CURRENT_VERSION = 1;

    private ServiceType type;
    
    private String uuid;

    private long version;

    private String name;

    private long last_updated_s;

    private Map<String, String> data;


    public ServiceRecord(Service service) {
        type = service.getType();
        uuid = service.getUuid();
        version = service.getVersion();
        name = service.getName();
        last_updated_s = service.getLastUpdatedS();
        data = new HashMap();
        for (KeyValuePair kvp : service.getData().getDataList()) {
            data.put(kvp.getKey(),kvp.getValue());
        }
    }

    public ServiceRecord(ReusableBuffer rb) throws IOException {
        byte recVersion = rb.get();

        if (recVersion == 1) {
            type = ServiceType.valueOf(rb.getInt());
            uuid = rb.getString();
            version = rb.getLong();
            name = rb.getString();
            last_updated_s = rb.getLong();
            final int numEntries = rb.getInt();
            data = new HashMap();
            for (int i = 0; i < numEntries; i++) {
                String key = rb.getString();
                String value = rb.getString();
                data.put(key, value);
            }
        } else {
            throw new IOException("don't know how to handle version "+recVersion);
        }
    }

    public Service getService() {
        Service.Builder s = Service.newBuilder();
        s.setType(type);
        s.setUuid(uuid);
        s.setVersion(version);
        s.setName(name);
        s.setLastUpdatedS(last_updated_s);
        ServiceDataMap.Builder sm = ServiceDataMap.newBuilder();
        for (Entry<String,String> e : data.entrySet()) {
            KeyValuePair kvp = KeyValuePair.newBuilder().setKey(e.getKey()).setValue(e.getValue()).build();
            sm.addData(kvp);
        }
        s.setData(sm);
        return s.build();
    }

    public int getSize() {
        final int INT_SIZE = Integer.SIZE/8;
        final int LONG_SIZE = Long.SIZE/8;
        final int BYTE_SIZE = Byte.SIZE/8;
        int size = BYTE_SIZE+INT_SIZE*4+(getData().size()*INT_SIZE*2)+LONG_SIZE*2+getUuid().length()+getName().length();
        for (Entry<String,String> e : getData().entrySet()) {
            size += e.getKey().length()+e.getValue().length();
        }
        return size;
    }

    public void serialize(ReusableBuffer rb) {

        rb.put(CURRENT_VERSION);
        rb.putInt(getType().getNumber());
        rb.putString(getUuid());
        rb.putLong(getVersion());
        rb.putString(getName());
        rb.putLong(getLast_updated_s());
        rb.putInt(getData().size());
        for (Entry<String,String> e : getData().entrySet()) {
            rb.putString(e.getKey());
            rb.putString(e.getValue());
        }

    }

    /**
     * @return the type
     */
    public ServiceType getType() {
        return type;
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
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the last_updated_s
     */
    public long getLast_updated_s() {
        return last_updated_s;
    }

    /**
     * @return the data
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * @param type the type to set
     */
    public void setType(ServiceType type) {
        this.type = type;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param last_updated_s the last_updated_s to set
     */
    public void setLast_updated_s(long last_updated_s) {
        this.last_updated_s = last_updated_s;
    }

    /**
     * @param data the data to set
     */
    public void setData(Map<String, String> data) {
        this.data = data;
    }

}
