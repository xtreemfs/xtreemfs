/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.operations;

import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.metadata.XLocList;
import org.xtreemfs.mrc.stages.XLocSetCoordinatorCallback;

public class InternalInstallXLocSetOperation extends MRCOperation {

    public InternalInstallXLocSetOperation(MRCRequestDispatcher master) {
        super(master);
    }

    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        XLocSetCoordinatorCallback callback = (XLocSetCoordinatorCallback) rq.getDetails().context
                .get("xLocSetCoordinatorCallback");

        if (callback != null) {
            callback.installXLocSet(rq, (String) rq.getDetails().context.get("fileId"),
                    (XLocList) rq.getDetails().context.get("extXLocList"));
        }
    }

}
