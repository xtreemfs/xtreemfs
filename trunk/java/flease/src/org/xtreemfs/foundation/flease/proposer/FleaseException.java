/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.foundation.flease.proposer;

/**
 *
 * @author bjko
 */
public class FleaseException extends Exception {

    public FleaseException(String message) {
        super(message);
    }

    public FleaseException(String message, Throwable cause) {
        super(message, cause);
    }

}
