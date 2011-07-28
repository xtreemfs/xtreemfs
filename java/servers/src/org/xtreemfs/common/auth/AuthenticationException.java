/*
 * Copyright (c) 2008 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.auth;

/**
 * Thrown by an authentication provide when authentication is not
 * possible for any reason.
 * @author bjko
 */
public class AuthenticationException extends Exception {

    /** creates a new exception.
     * 
     * @param msg an error message that should be meaningful to users!
     */
    public AuthenticationException(String msg) {
        super(msg);
    }
}
