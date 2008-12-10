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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.common.buffer;

import java.nio.ByteBuffer;

/** This class contains some convenience methods for very diverses uses
 *
 * @author Jesus Malo (jmalo)
 */
public class BufferConversionUtils {
    
    /**
     * Creates a new instance of BufferConversionUtils
     */
    public BufferConversionUtils() {
    }
    
    /** It gets the array of bytes of a ByteBuffer
     *  @param source The object containing the require array of bytes
     *  @return The array of bytes contained in the given ByteBuffer
     */
    public static byte [] arrayOf(ByteBuffer source) {
        byte [] array;
        
        if (source.hasArray()) {
            array = source.array();
        } else {
            array = new byte[source.capacity()];
            source.position(0);
            source.get(array);
        }
        
        return array;
    }
     
    
}
