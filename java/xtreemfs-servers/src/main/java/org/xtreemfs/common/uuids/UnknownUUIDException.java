/*
 * Copyright (c) 2008-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.common.uuids;

import java.io.IOException;

/**
 * Thrown when a UUID cannot be mapped to a service's InetSocketAddress and Protoco.
 * @author bjko
 */
public class UnknownUUIDException extends IOException {

    public UnknownUUIDException(String message) {
        super(message);
    }
    
}
