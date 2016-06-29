/*
 * Copyright (c) 2008-2010 by Bjoern Kolbeck, Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.json;

/**
 * Thrown by the JSON parser and writer.
 * 
 * @author bjko
 */
public class JSONException extends java.lang.Exception {

    /***/
    private static final long serialVersionUID = 2422241603599209392L;

    /**
     * Creates a new instance of <code>JSONException</code> without detail
     * message.
     */
    public JSONException() {
    }

    /**
     * Constructs an instance of <code>JSONException</code> with the specified
     * detail message.
     * 
     * @param msg
     *            the detail message.
     */
    public JSONException(String msg) {
        super(msg);
    }
}
