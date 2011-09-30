/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.dir.data;

import java.io.IOException;
import java.util.Vector;

import org.xtreemfs.foundation.buffer.ReusableBuffer;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Configuration;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;

public class ConfigurationRecord {

    public static final byte CURRENT_VERSION = 1;
    
    private String                  uuid;
    
    /**
     * 
     */
    private long version;

    private Vector<KeyValuePair> configurationParameter;

    public ConfigurationRecord(Configuration c) {
        this.uuid = c.getUuid();
        this.version = c.getVersion();
        this.configurationParameter = new Vector<KeyValuePair>();
        for (KeyValuePair kvp : c.getParameterList()) {
            configurationParameter.add(kvp);
        }
    }

    public ConfigurationRecord(ReusableBuffer rb) throws IOException {
        byte recVersion = rb.get();
        
        if (recVersion == 1) {
            this.uuid = rb.getString();
            this.version = rb.getLong();
            
            this.configurationParameter = new Vector<KeyValuePair>();
            while (rb.remaining() != 0) {
                configurationParameter.add(KeyValuePair.newBuilder().setKey(rb.getString())
                		.setValue(rb.getString()).build());
                		
            }            
        } else {
            throw new IOException("don't know how to handle version "+recVersion);
        }
       
    }

    public void serialize(ReusableBuffer rb) {
        rb.put(CURRENT_VERSION);
        rb.putString(uuid);
        rb.putLong(getVersion());
        
        for (KeyValuePair kvp : configurationParameter) {
            rb.putString(kvp.getKey());
            rb.putString(kvp.getValue());
        }
    }

    public String getUuid() {
        return uuid;
    }

    public Configuration getConfiguration() {
        Configuration.Builder conf = Configuration.newBuilder();
        
        conf.setUuid(this.getUuid()).setVersion(this.getVersion());
        
        for (KeyValuePair kvp : configurationParameter) {
            conf.addParameter(kvp);
        }
        
        return conf.build();

    }
    
    
    public int getSize() {
        
        final int BYTE_SIZE = Byte.SIZE/8;
        final int INT_SIZE = Integer.SIZE/8;
        final int LONG_SIZE = Long.SIZE/8;
        
        
        //length of the uuid + 1*INT_SIZE for the uuid datatype
        int size = getUuid().length() + INT_SIZE; 
        //+size of the version
        size += LONG_SIZE;
        //+size of the CURRENT_VERSION
        size += BYTE_SIZE;
        //+size of the data HashMap
        size += (getData().size()*INT_SIZE*2);
        
        //+size of the values in the HashMap
        for (KeyValuePair kvp : configurationParameter) {
            size += kvp.getKey().length() + kvp.getValue().length();
        }  
        
        return size;
    }
    
    public Vector<KeyValuePair> getData() {
        return this.configurationParameter;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getVersion() {
        return version;
    }
}
