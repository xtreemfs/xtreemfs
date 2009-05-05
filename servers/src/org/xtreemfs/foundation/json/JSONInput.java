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
 * AUTHORS: BjÃ¶rn Kolbeck (ZIB), Jan Stender (ZIB), JesÃºs Malo (BSC)
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
