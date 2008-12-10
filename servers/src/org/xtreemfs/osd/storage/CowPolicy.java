/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.storage;

/**
 * This class implements differen copy-on-write strategies.
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
         * Create a copy only for the first write after open,
         * then overwrite for each subsequent request.
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
     * The initial number of objects. Required for
     * COW_ONCE mode.
     */
    private final long    initialObjectCount;
    
    /**
     * per-object flag which indicates if the object has already been copied
     * (for COW_ONCE mode). Each bit is used
     */
    private final byte[]  cowFlags;
    
    /**
     * Create a new cowPolicy
     * @param mode can be NO_COW or ALWAYS_COW but not COW_ONCE
     */
    public CowPolicy(cowMode mode) {
        if (mode == cowMode.COW_ONCE) {
            throw new IllegalArgumentException("Mode COW_ONCE requires initial object count!");
        }
        this.mode = mode;
        this.initialObjectCount = 0;
        this.cowFlags = null;
    }
    
    /**
     * Creates a new cowPolicy with COW_ONCE
     * @param mode mut be COW_ONCE
     * @param initialObjectCount the number of objects when openening the file (added objects do not require COW)
     */
    public CowPolicy(cowMode mode, long initialObjectCount) {
        this.mode = mode;
        this.initialObjectCount = initialObjectCount;
        final int fieldLen = (int) Math.ceil(initialObjectCount/Byte.SIZE);
        cowFlags = new byte[fieldLen];
    }
    
    /**
     * Checks if an object must be copied before writing
     * @param objectNumber the object to be modified
     * @return true, if a new version must be created
     */
    private boolean requiresCow(int objectNumber) {
        assert(mode == cowMode.COW_ONCE);
        //new objects do not need copy-on-write ;-)
        if (objectNumber >= this.initialObjectCount)
            return false;
        
        final int field = objectNumber / Byte.SIZE;
        final int bit = objectNumber % Byte.SIZE;
        return (cowFlags[field] & (0x0001 << bit)) == 0;
    }
    
    /**
     * Checks if copy-on-write is necessary for an object
     * @param objectNumber the object to be modified
     * @return true, if a new version must be created
     */
    public boolean isCOW(int objectNumber) {
        return ((mode != cowMode.NO_COW) && 
                ((mode == cowMode.ALWAYS_COW) || requiresCow(objectNumber)) );
    }
    
    /**
     * toggels the written flag for an object if in COW_ONMCE mode
     * @param objectNumber the object which was modified
     */
    public void objectChanged(int objectNumber) {
        //ignore new objects
        if ((mode == cowMode.COW_ONCE) && (objectNumber < this.initialObjectCount)) {
            final int field = objectNumber / Byte.SIZE;
            final int bit = objectNumber % Byte.SIZE;
            cowFlags[field] = (byte)(cowFlags[field] | (0x01 << bit));
        }
    }
    
}
