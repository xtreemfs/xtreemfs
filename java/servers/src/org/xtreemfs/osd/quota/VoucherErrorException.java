/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.quota;

/**
 * Specific exception type for quota handling and catching an error properly.
 */
public class VoucherErrorException extends Exception {

    private static final long serialVersionUID = -7983169209931452497L;

    public VoucherErrorException() {
    }

    public VoucherErrorException(String message) {
        super(message);
    }

    public VoucherErrorException(Throwable cause) {
        super(cause);
    }

    public VoucherErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
