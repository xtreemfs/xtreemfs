/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.json;


/**
 * This class is necessary because the StringReader cannot skip back once the
 * end is reached.
 * 
 * @author bjko
 */
public class JSONString implements JSONInput {

    String str;

    int position;

    /**
     * Creates a new instance of JSONString
     * 
     * @param str
     *            the JSON message
     */
    public JSONString(String str) {
        this.str = str;
        position = 0;
    }

    /**
     * reads a single char
     * 
     * @return the character at the current position is returned
     */
    public char read() throws JSONException {
        try {
            return str.charAt(position++);
        }
        catch (StringIndexOutOfBoundsException ex) {
            throw new JSONException("Reach the end of the string");
        }
    }

    /**
     */
    public boolean hasMore() {
        return position < str.length();
    }
    
    
    /**
     * Skips skip characters
     * 
     * @param skip
     *            num characters to skip
     * @return the number of characters skipped
     */
    public int skip(int skip) {
        if (((position + skip) < 0) || ((position + skip) >= str.length())) {
            return 0;
        } else {
            position = position + skip;
            return skip;
        }
    }

    /**
     * Get a string representation.
     * 
     * @return A string representation of the this JSONString.
     */
    public String toString() {
        return "JSONString pos=" + position + "   str=" + str;
    }
    
}
