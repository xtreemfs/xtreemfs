/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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
 * AUTHORS: Bjoern Kolbeck (ZIB), Jan Stender (ZIB)
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
