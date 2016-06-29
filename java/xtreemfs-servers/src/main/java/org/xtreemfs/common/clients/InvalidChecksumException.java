/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.clients;

import java.io.IOException;

/**
 *
 * @author bjko
 */
public class InvalidChecksumException extends IOException {

    public InvalidChecksumException(String message) {
        super(message);
    }

}
