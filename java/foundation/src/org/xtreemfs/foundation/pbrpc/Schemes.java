/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.pbrpc;

/**
 *
 * @author bjko
 */
public class Schemes {

    public static final String SCHEME_PBRPC = "pbrpc";
    public static final String SCHEME_PBRPCG = "pbrpcg";
    public static final String SCHEME_PBRPCS = "pbrpcs";
    public static final String SCHEME_PBRPCU = "pbrpcu";

    public static String getScheme(boolean sslEnabled, boolean gridSSL) {
        if (sslEnabled) {
            if (gridSSL) {
                return SCHEME_PBRPCG;
            } else {
                return SCHEME_PBRPCS;
            }
        } else {
            return SCHEME_PBRPC;
        }
    }

}
