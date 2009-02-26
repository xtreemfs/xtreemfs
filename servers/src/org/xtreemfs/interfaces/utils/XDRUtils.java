/*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Minor Gordon (NEC), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.interfaces.utils;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.foundation.oncrpc.utils.ONCRPCBufferWriter;

/**
 *
 * @author bjko
 */
public class XDRUtils {

    /**
     * Maximum length of a string that the parser accepts
     */
    public static final int MAX_STRLEN = 32*1024;

    /**
     * Maximum number of elements in an array that the parser
     * accepts
     */
    public static final int MAX_ARRAY_ELEMS = 8*1024;


    public static String deserializeString(ReusableBuffer buf) {
        final int strlen = buf.getInt();
        if (strlen > MAX_STRLEN)
            throw new IllegalArgumentException("string is too large ("+strlen+"), maximum allowed is "+MAX_STRLEN+" bytes");
        byte[] bytes = new byte[strlen];
        buf.get(bytes);
        final String str = new String(bytes);
        if (strlen% 4 > 0) {
            for (int k = 0; k < (4 - (strlen % 4)); k++) {
                buf.get();
            }
        }
        return str;
    }

    public static void serializeString(String str, ONCRPCBufferWriter writer) {
        final int strlen = str.length();
        if (strlen > MAX_STRLEN)
            throw new IllegalArgumentException("string is too large ("+strlen+"), maximum allowed is "+MAX_STRLEN+" bytes");
        final byte[] bytes = str.getBytes();
        writer.putInt(strlen);
        writer.put(bytes);
        if (strlen% 4 > 0) {
            for (int k = 0; k < (4 - (strlen % 4)); k++) {
                writer.put((byte)0);
            }
        }
    }


}
