/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin and
 Consiglio Nazionale delle Ricerche.

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
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB), Eugenio Cesario (CNR)
 */

package org.xtreemfs.osd.stages;

import java.io.IOException;

import org.xtreemfs.osd.OSDRequest;
import org.xtreemfs.osd.RequestDispatcher;
import org.xtreemfs.osd.storage.MetadataCache;
import org.xtreemfs.osd.storage.StorageLayout;
import org.xtreemfs.osd.storage.Striping;

public class StorageStage extends Stage {

    private StorageThread[] storageThreads;

    /** Creates a new instance of MultithreadedStorageStage */
    public StorageStage(RequestDispatcher master, Striping striping,
        MetadataCache cache, StorageLayout layout, int numOfThreads)
        throws IOException {

        super("OSD Storage Stage");

        int numberOfThreads = 5;
        if (numOfThreads > 0)
            numberOfThreads = numOfThreads;

        storageThreads = new StorageThread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++)
            storageThreads[i] = new StorageThread(i, master, striping, cache,
                layout);
    }

    public void enqueueOperation(OSDRequest rq, int method,
        StageCallbackInterface callback) {

        rq.setEnqueueNanos(System.nanoTime());
        
        // choose the thread the new request has to be
        // assigned to, for its execution
        int taskId = getTaskId(rq);

        // add the new request to the storageTask,
        // in order to start/schedule its execution
        // concurrently with other threads assigned to other
        // storageTasks
        storageThreads[taskId].enqueueOperation(rq, method, callback);
    }

    @Override
    protected void processMethod(StageMethod method) {
        // empty, processing takes place in storage thread
    }

    public void run() {
        // start all storage threads
        for (StorageThread th : storageThreads)
            th.start();
    }

    public void shutdown() {
        for (StorageThread th : storageThreads)
            th.shutdown();
    }

    public void waitForStartup() throws Exception {
        // wait for all storage threads to be ready
        for (StorageThread th : storageThreads)
            th.waitForStartup();
    }

    public void waitForShutdown() throws Exception {
        // wait for all storage threads to be shut down
        for (StorageThread th : storageThreads)
            th.waitForShutdown();
    }

    private int getTaskId(OSDRequest rq) {

        // calculate a hash value from the file ID and return the responsible
        // thread
        String fileId = rq.getDetails().getFileId()
            + rq.getDetails().getObjectNumber();
        int key = Math.abs(fileId.hashCode());
        int index = (key % storageThreads.length);

        // String objId = rq.getDetails().getFileId()
        // + rq.getDetails().getObjectNumber();
        // int key = Math.abs(objId.hashCode());
        // int index = (key % storageThreads.length);

        return index;
    }

}
