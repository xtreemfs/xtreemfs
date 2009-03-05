///*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
//    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
//    Grid Operating System, see <http://www.xtreemos.eu> for more details.
//    The XtreemOS project has been developed with the financial support of the
//    European Commission's IST program under contract #FP6-033576.
//
//    XtreemFS is free software: you can redistribute it and/or modify it under
//    the terms of the GNU General Public License as published by the Free
//    Software Foundation, either version 2 of the License, or (at your option)
//    any later version.
//
//    XtreemFS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Nele Andersen (ZIB)
// */
//package org.xtreemfs.common.clients.scrubber;
//
//import java.net.InetSocketAddress;
//
//import org.xtreemfs.common.clients.RPCResponse;
//import org.xtreemfs.common.clients.RPCResponseListener;
//import org.xtreemfs.common.clients.io.RandomAccessFile;
//import org.xtreemfs.common.clients.osd.OSDClient;
//import org.xtreemfs.common.uuids.ServiceUUID;
//
///**
// *
// *
// */
//public class ScrubbedFile {
//    private AsyncScrubber scrubber;
//    private RandomAccessFile file;
//    private long expectedFileSize;
//    private FileState fileState;
//    private boolean stopIssuingRequests = false;
//
//    public ScrubbedFile(AsyncScrubber scrubber, RandomAccessFile file)
//                throws Exception {
//        this.scrubber = scrubber;
//        this.file = file;
//        this.expectedFileSize = file.length();
//        int expectedNoOfObject =
//            (int) (expectedFileSize / file.getStripeSize()) + 1;
//        fileState = new FileState(file.getStripeSize(), expectedNoOfObject);
//    }
//
//    /**
//     * Called by Main thread.
//     * @return returns the next object stored on the osd specified by the
//     * parameter osd which has not been read or -1 if the file is marked as
//     * unreadable or has no unread objects stored on the osd.
//     */
//    public int getRequestForOSD(OSDWorkQueue osd) {
//        // check if osd is in StripingPolicy list of osds for this file
//        if(stopIssuingRequests || !file.getOSDs().contains(osd.getOSDId()))
//            return -1;
//
//        // find next object which has not been read for the osd
//        for(int i = 0; i < fileState.getNoOfObjectStates(); i++){
//            if (fileState.isTodo(i) && file.getOSDId(i).equals(osd.getOSDId())){
//                return i;
//            }
//        }
//        return -1;
//    }
//    /**
//     * Called by Multispeedy or Main thread.
//     * Sets the object state to READING
//     */
//    public void markObjectAsInFlight(int objectNo){
//        fileState.markObjectAsInFlight(objectNo);
//    }
//
//    /**
//     * Called by Multispeedy or Main thread.
//     * Is only invoked after successfully reading the object
//     * @param bytesInObject the number of bytes read
//     * @param objectNo the object which has been read
//     */
//    public void objectHasBeenRead(long bytesInObject, int objectNo) {
//        fileState.incorporateReadResult(objectNo, bytesInObject);
//        if(fileState.isFileDone())
//            scrubber.fileFinished(this, fileState.getFileSize(),false);
//    }
//    /**
//     * Called by Multispeedy or Main thread.
//     * Marks the file as unreadable and removes the file from the scrubbers
//     * currentFiles list.
//     * @param objectNo
//     */
//    public void couldNotReadObject(int objectNo) {
//        stopIssuingRequests = true;
//        scrubber.fileFinished(this, fileState.getFileSize(),true);
//    }
//
//    /***
//     *
//     * @param objectNo
//     */
//
//    public void objectHasInvalidChecksum(int objectNo) {
//        scrubber.foundInvalidChecksum(this,objectNo);
//    }
//
//
//    /**
//     * @TODO logging... ist es notwendig hier... wird es nicht in responseAvailable gemacht?
//     * Called by Main thread.
//     * Sends an checkObject request to the osd holding the object specified by objectNo.
//     * @param osdClient
//     * @param listener
//     * @param context
//     * @param objectNo
//     */
//    public RPCResponse readObjectAsync(OSDClient osdClient,
//                               RPCResponseListener listener,
//                               ReadObjectContext context,
//                               int objectNo) {
//        RPCResponse response = null;
//        try {
//            ServiceUUID osd = file.getOSDId(objectNo);
//            InetSocketAddress current_osd_address = osd.getAddress();
//            response = osdClient.checkObject(current_osd_address, file.getLocations(),
//                    file.getCapability(), file.getFileId(), objectNo);
//            response.setAttachment(context);
//            response.setResponseListener(listener);
//        }catch(Exception e){
//            e.printStackTrace();
//            // log "Exception thrown while attempting to read object no. ... of file ... "
//        }
//        return response;
//    }
//
//    public String getPath(){
//        return file.getPath();
//    }
//
//    public long getExpectedFileSize(){
//        return expectedFileSize;
//    }
//}