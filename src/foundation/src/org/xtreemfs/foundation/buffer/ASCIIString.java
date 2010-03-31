/*
 * Copyright (c) 2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
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
 * AUTHORS: Björn Kolbeck (ZIB), Jan Stender (ZIB)
 */

package org.xtreemfs.foundation.buffer;

import java.io.Serializable;

/**
 *
 * @author bjko
 */
public final class ASCIIString implements Serializable {
    private static final long serialVersionUID = 4633232360908659139L;

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

}
