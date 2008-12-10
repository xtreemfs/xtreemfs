/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
    Barcelona Supercomputing Center - Centro Nacional de Supercomputacion.

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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB), Jesús Malo (BSC)
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

    /** 
     *  @author Jesús Malo (jmalo)
     */
    public boolean equals(Object obj) {               
        if(this == obj) return true;        
        if((obj == null) || (obj.getClass() != this.getClass())) return false;
        
        JSONString other = (JSONString) obj;
        
        String Iam = toString();
        String ItIs = other.toString();
        
        return Iam.equals(ItIs);        
    }
    
    /** 
     *  @author Jesús Malo (jmalo)
     */
    public int hashCode() {
        return toString().hashCode();
    }
    
    /** It provides the String of the JSONString
     *  @author Jesús Malo (jmalo)
     *  @return The JSONString of the object
     */
    public String asString() {
        return str;
    }
}
