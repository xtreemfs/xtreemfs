/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.new_mrc.metadata;

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
