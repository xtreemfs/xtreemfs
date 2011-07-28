/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedStripingPolicy extends BufferBackedMetadata implements StripingPolicy {
    
    private static final int SIZE_INDEX    = 0;
    
    private static final int WIDTH_INDEX   = 4;
    
    private static final int PATTERN_INDEX = 8;
    
    private String           pattern;
    
    private int              stripeSize;
    
    private int              width;
    
    public BufferBackedStripingPolicy(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }
    
    public BufferBackedStripingPolicy(byte[] buffer, int offset, int len) {
        
        super(buffer, offset, len);
        
        this.pattern = new String(buffer, offset + PATTERN_INDEX, len - PATTERN_INDEX);
        
        ByteBuffer tmp = ByteBuffer.wrap(buffer, offset + SIZE_INDEX, Integer.SIZE / 8);
        this.stripeSize = tmp.getInt();
        
        tmp = ByteBuffer.wrap(buffer, offset + WIDTH_INDEX, Integer.SIZE / 8);
        this.width = tmp.getInt();
    }
    
    public BufferBackedStripingPolicy(String pattern, int stripeSize, int width) {
        
        super(null, 0, 0);
        
        len = pattern.getBytes().length + 8;
        buffer = new byte[len];
        ByteBuffer tmp = ByteBuffer.wrap(buffer);
        tmp.putInt(stripeSize).putInt(width).put(pattern.getBytes());
        
        this.pattern = pattern;
        this.stripeSize = stripeSize;
        this.width = width;
    }
    
    public boolean equals(StripingPolicy pol) {
        return toString().equals(pol.toString());
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public int getStripeSize() {
        return stripeSize;
    }
    
    public int getWidth() {
        return width;
    }
    
}
