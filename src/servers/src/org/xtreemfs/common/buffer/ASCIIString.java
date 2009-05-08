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

import java.io.Serializable;

/**
 *
 * @author bjko
 */
public final class ASCIIString implements Serializable {

    private byte[] data;

    private int hash;

    protected ASCIIString() {

    }

    /**
     * Creates a new instance of ASCIIString
     */
    public ASCIIString(String str) {
        this.data = str.getBytes();
    }

    /**
     * Creates a new instance of ASCIIString
     */
    protected ASCIIString(byte[] data) {
        this.data = data;
    }

    public String toString() {
        return new String(data);
    }

    public char charAt(int index) {

        return (char)data[index];
    }

    private byte unckeckedGetByte(int index) {
        return data[index];
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        try {
            ASCIIString other = (ASCIIString)o;

            for (int i = 0; i < data.length; i++) {
                if (this.unckeckedGetByte(i) != other.unckeckedGetByte(i))
                    return false;
            }
            return true;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    public void marshall(ReusableBuffer target) {
        target.putInt(data.length);
        target.put(data);
        
    }

    public static ASCIIString unmarshall(ReusableBuffer target) {

        int length = target.getInt();
        if (length < 0)
            return null;
        byte[] tmp = new byte[length];

        target.get(tmp);
        
        return new ASCIIString(tmp);
    }

    public int hashCode() {
	int h = hash;
	if (h == 0) {

            for (int i = 0; i < data.length; i++) {
                h = 31*h + data[i];
            }
            hash = h;
        }
        return h;
    }

}
