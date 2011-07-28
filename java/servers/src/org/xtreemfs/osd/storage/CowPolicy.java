/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

/**
 * This class implements differen copy-on-write strategies.
 * 
 * @author bjko
 */
public class CowPolicy {
    
    public static final CowPolicy PolicyNoCow = new CowPolicy(cowMode.NO_COW);
    
    public enum cowMode {
        /**
         * Do not copy on write, overwrite instead.
         */
        NO_COW,
        /**
         * Create a copy only for the first write after open, then overwrite for
         * each subsequent request.
         */
        COW_ONCE,
        /**
         * Copy-on-write for all write requests.
         */
        ALWAYS_COW
    };
    
    /**
     * CoW mode to use
     */
    private final cowMode mode;
    
    /**
     * The initial number of objects. Required for COW_ONCE mode.
     */
    private long          initialObjectCount;
    
    /**
     * per-object flag which indicates if the object has already been copied
     * (for COW_ONCE mode). Each bit is used
     */
    private byte[]        cowFlags;
    
    /**
     * Create a new cowPolicy
     * 
     * @param mode
     *            the COW mode
     */
    public CowPolicy(cowMode mode) {
        this.mode = mode;
    }
    
    /**
     * Initializes the COW flags if necessary.
     * 
     * @param objectCount
     *            the current number of objects
     */
    public void initCowFlagsIfRequired(long objectCount) {
        
        if (mode == cowMode.COW_ONCE && cowFlags == null) {
            
            assert (objectCount / Byte.SIZE < Integer.MAX_VALUE) : "number of objects for COW_ONCE file ("
                + objectCount + ") exceeds limit (" + Integer.MAX_VALUE * Byte.SIZE + ")";
            
            final int fieldLen = (int) Math.ceil((double) objectCount / Byte.SIZE);
            cowFlags = new byte[fieldLen];
            initialObjectCount = objectCount;
        }
    }
    
    /**
     * Checks if an object must be copied before writing
     * 
     * @param objectNumber
     *            the object to be modified
     * @return true, if a new version must be created
     */
    private boolean requiresCow(int objectNumber) {
        
        assert (mode == cowMode.COW_ONCE);
        assert (cowFlags != null);
        
        // new objects do not need copy-on-write ;-)
        if (objectNumber >= initialObjectCount)
            return false;
        
        final int field = objectNumber / Byte.SIZE;
        final int bit = objectNumber % Byte.SIZE;
        return (cowFlags[field] & (0x0001 << bit)) == 0;
    }
    
    /**
     * Checks if copy-on-write is necessary for an object
     * 
     * @param objectNumber
     *            the object to be modified
     * @return true, if a new version must be created
     */
    public boolean isCOW(int objectNumber) {
        return ((mode != cowMode.NO_COW) && ((mode == cowMode.ALWAYS_COW) || requiresCow(objectNumber)));
    }
    
    /**
     * Checks if copy-on-write is generally enabled.
     * 
     * @return true, if a COW mode other than NO_COW was assigned to the policy
     */
    public boolean cowEnabled() {
        return mode != cowMode.NO_COW;
    }
    
    /**
     * toggels the written flag for an object if in COW_ONMCE mode
     * 
     * @param objectNumber
     *            the object which was modified
     */
    public void objectChanged(int objectNumber) {
        
        // ignore new objects
        if ((mode == cowMode.COW_ONCE) && (objectNumber < initialObjectCount)) {
            assert (cowFlags != null);
            final int field = objectNumber / Byte.SIZE;
            final int bit = objectNumber % Byte.SIZE;
            cowFlags[field] = (byte) (cowFlags[field] | (0x01 << bit));
        }
    }
    
}
