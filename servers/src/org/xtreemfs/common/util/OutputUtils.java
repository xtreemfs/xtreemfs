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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Jesús Malo (BSC)
 */

package org.xtreemfs.common.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * 
 * @author bjko
 */
public final class OutputUtils {
    
    public static final char[] trHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
        'B', 'C', 'D', 'E', 'F'     };

    public static final byte[] fromHex;

    static {
        fromHex = new byte[128];
        fromHex['0'] = 0;
        fromHex['1'] = 1;
        fromHex['2'] = 2;
        fromHex['3'] = 3;
        fromHex['4'] = 4;
        fromHex['5'] = 5;
        fromHex['6'] = 6;
        fromHex['7'] = 7;
        fromHex['8'] = 8;
        fromHex['9'] = 9;
        fromHex['A'] = 10;
        fromHex['a'] = 10;
        fromHex['B'] = 11;
        fromHex['b'] = 11;
        fromHex['C'] = 12;
        fromHex['c'] = 12;
        fromHex['D'] = 13;
        fromHex['d'] = 13;
        fromHex['E'] = 14;
        fromHex['e'] = 14;
        fromHex['F'] = 15;
        fromHex['f'] = 15;

    }
    
    public static final String byteToHexString(byte b) {
        StringBuilder sb = new StringBuilder(2);
        sb.append(trHex[((b >> 4) & 0x0F)]);
        sb.append(trHex[(b & 0x0F)]);
        return sb.toString();
    }
    
    public static final String byteArrayToHexString(byte[] array) {
        StringBuilder sb = new StringBuilder(2 * array.length);
        for (byte b : array) {
            sb.append(trHex[((b >> 4) & 0x0F)]);
            sb.append(trHex[(b & 0x0F)]);
        }
        return sb.toString();
    }
    
    public static final String byteArrayToFormattedHexString(byte[] array) {
        return byteArrayToFormattedHexString(array, 0, array.length);
    }
    
    public static final String byteArrayToFormattedHexString(byte[] array, int offset, int len) {
        StringBuilder sb = new StringBuilder(2 * len);
        for (int i = offset; i < offset + len; i++) {
            sb.append(trHex[((array[i] >> 4) & 0x0F)]);
            sb.append(trHex[(array[i] & 0x0F)]);
            if ((i - offset) % 4 == 3) {
                if ((i - offset) % 16 == 15)
                    sb.append("\n");
                else
                    sb.append(" ");
            }
            
        }
        return sb.toString();
    }
    
    public static final String stackTraceToString(Throwable th) {
        
        PrintStream ps = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ps = new PrintStream(out);
            if (th != null)
                th.printStackTrace(ps);
            
            return new String(out.toByteArray());
            
        } finally {
            if (ps != null)
                ps.close();
        }
        
    }
    
    public static String formatBytes(long bytes) {
        
        double kb = bytes / 1024.0;
        double mb = bytes / (1024.0 * 1024.0);
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        double tb = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
        
        if (tb >= 1.0) {
            return String.format("%.2f TB", tb);
        } else if (gb >= 1.0) {
            return String.format("%.2f GB", gb);
        } else if (mb >= 1.0) {
            return String.format("%.2f MB", mb);
        } else if (kb >= 1.0) {
            return String.format("%.2f kB", kb);
        } else {
            return bytes + " bytes";
        }
    }
    
    public static String escapeToXML(String st) {
        st = st.replace("&", "&amp;");
        st = st.replace("'", "&apos;");
        st = st.replace("<", "&lt;");
        st = st.replace(">", "&gt;");
        st = st.replace("\"", "&quot;");
        return st;
    }
    
    public static String unescapeFromXML(String st) {
        st = st.replace("&amp;", "&");
        st = st.replace("&apos;", "'");
        st = st.replace("&lt;", "<");
        st = st.replace("&gt;", ">");
        st = st.replace("&quot;", "\"");
        return st;
    }
    
    public static byte[] hexStringToByteArray(String hexString) {
        
        assert (hexString.length() % 2 == 0);
        byte[] bytes = new byte[hexString.length() / 2];
        
        for (int i = 0; i < hexString.length(); i += 2) {
            int b = Integer.parseInt(hexString.substring(i, i + 2), 16);
            bytes[i / 2] = b >= 128 ? (byte) (b - 256) : (byte) b;
        }
        
        return bytes;
    }

    /**
     * Writes an integer as a hex string to sb starting with the LSB.
     * @param sb
     * @param value
     */
    public static void writeHexInt(final StringBuffer sb, final int value) {
        sb.append(OutputUtils.trHex[(value & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 4) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 8) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 12) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 16) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 20) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 24) & 0x0F)]);
        sb.append(OutputUtils.trHex[((value >> 28) & 0x0F)]);
    }

    public static void writeHexLong(final StringBuffer sb, final long value) {
        OutputUtils.writeHexInt(sb,(int) (value & 0xFFFFFFFF));
        OutputUtils.writeHexInt(sb,(int) (value >> 32));
    }

    /**
     * Reads an integer from a hex string (starting with the LSB).
     * @param str
     * @param position
     * @return
     */
    public static int readHexInt(final String str, int position) {
        int value = OutputUtils.fromHex[str.charAt(position)];
        value += ((int)OutputUtils.fromHex[str.charAt(position+1)]) << 4;
        value += ((int)OutputUtils.fromHex[str.charAt(position+2)]) << 8;
        value += ((int)OutputUtils.fromHex[str.charAt(position+3)]) << 12;
        value += ((int)OutputUtils.fromHex[str.charAt(position+4)]) << 16;
        value += ((int)OutputUtils.fromHex[str.charAt(position+5)]) << 20;
        value += ((int)OutputUtils.fromHex[str.charAt(position+6)]) << 24;
        value += ((int)OutputUtils.fromHex[str.charAt(position+7)]) << 28;

        return value;
    }

    public static long readHexLong(final String str, int position) {
        long value = OutputUtils.readHexInt(str, position+8);
        int tmp = OutputUtils.readHexInt(str, position);
        value += ((long)tmp)<< 32;
        return value;
    }
}
