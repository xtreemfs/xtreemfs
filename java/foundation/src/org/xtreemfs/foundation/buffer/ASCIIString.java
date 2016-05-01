/*
 * Copyright (c) 2010 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.buffer;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 *
 * @author bjko
 */
public final class ASCIIString implements Serializable {
    private static final long    serialVersionUID = 4633232360908659139L;

    private static final Charset charset          = Charset.forName("US-ASCII");

    private byte[]               data;

    private int                  hash;

    protected ASCIIString() {

    }

    /**
     * Creates a new instance of ASCIIString
     */
    public ASCIIString(String str) {
        this.data = str.getBytes(charset);
    }

    /**
     * Creates a new instance of ASCIIString
     */
    protected ASCIIString(byte[] data) {
        this.data = data;
    }

    public String toString() {
        return new String(data, charset);
    }

    public char charAt(int index) {

        return (char)data[index];
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        try {
            ASCIIString other = (ASCIIString)o;

            if (other.length() != this.length())
                return false;

            for (int i = 0; i < data.length; i++) {
                if (data[i] != other.data[i])
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

    public int length() {
        return data.length;
    }

    public int getSerializedSize() {
        return length()+Integer.SIZE/8;
    }
    
    /**
     * Splits the string on the last occurrence of the separator character.
     * 
     * @param separator
     *            Character to split the string at
     * @param include
     *            If set to true, the separator is included in the first result
     * @return ASCIIString[] of size two if the separator is found, otherwise the current string is set as only element
     */
    public ASCIIString[] splitLast(byte separator, boolean include) {
        int m = (include ? 1 : 0);

        for (int i = data.length - 1; i >= 0; i--) {
            if (data[i] == separator) {
                return new ASCIIString[]{
                        new ASCIIString(Arrays.copyOfRange(data, 0, i + m)),
                        new ASCIIString(Arrays.copyOfRange(data, i + 1, data.length)) };
            }
        }
        return new ASCIIString[] { this };
    }

}
