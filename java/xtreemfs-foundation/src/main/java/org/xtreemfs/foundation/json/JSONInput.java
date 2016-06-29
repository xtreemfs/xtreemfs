/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.json;

/**
 *
 * @author bjko
 */
public interface JSONInput {
    
    
    /**
     * reads a single char
     * 
     * @return the character at the current position is returned
     */
    public char read() throws JSONException;
    
    /**
     *  It checks if there are more characters
     */
    public boolean hasMore();
    
    /**
     * Skips skip characters
     * 
     * @param skip
     *            num characters to skip
     * @return the number of characters skipped
     */
    public int skip(int skip);
    
    /**
     * Get a string representation.
     * 
     * @return A string representation of the this JSONString.
     */
    public String toString();
    
}
