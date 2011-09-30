/*
 * Copyright (c) 2010-2011 by Bjoern Kolbeck,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.rwre;

/**
 *
 * @author bjko
 */
public class RedirectToMasterException extends Exception {

    final String masterUUID;

    public RedirectToMasterException(String masterUUID) {
        super();
        this.masterUUID = masterUUID;
    }

    public String getMasterUUID() {
        return masterUUID;
    }

}
