///*  Copyright (c) 2009 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
//
// This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
// Grid Operating System, see <http://www.xtreemos.eu> for more details.
// The XtreemOS project has been developed with the financial support of the
// European Commission's IST program under contract #FP6-033576.
//
// XtreemFS is free software: you can redistribute it and/or modify it under
// the terms of the GNU General Public License as published by the Free
// Software Foundation, either version 2 of the License, or (at your option)
// any later version.
//
// XtreemFS is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
// */
///*
// * AUTHORS: Björn Kolbeck (ZIB)
// */
//
//package org.xtreemfs.osd.operations;
//
//import org.xtreemfs.common.Capability;
//import org.xtreemfs.foundation.buffer.ReusableBuffer;
//import org.xtreemfs.common.uuids.ServiceUUID;
//import org.xtreemfs.common.xloc.XLocations;
//import org.xtreemfs.interfaces.OSDInterface.keep_file_openRequest;
//import org.xtreemfs.interfaces.OSDInterface.keep_file_openResponse;
//import org.xtreemfs.foundation.oncrpc.utils.Serializable;
//import org.xtreemfs.osd.OSDRequest;
//import org.xtreemfs.osd.OSDRequestDispatcher;
//
///**
// *
// * @author bjko
// */
//public class KeepFileOpenOperation extends OSDOperation {
//
//    private final int procId;
//
//    private final String sharedSecret;
//
//    private final ServiceUUID localUUID;
//
//    private final keep_file_openResponse response;
//
//    public KeepFileOpenOperation(OSDRequestDispatcher master) {
//        super(master);
//        keep_file_openRequest rq = new keep_file_openRequest();
//        response = new keep_file_openResponse();
//        sharedSecret = master.getConfig().getCapabilitySecret();
//        localUUID = master.getConfig().getUUID();
//    }
//
//    @Override
//    public int getProcedureId() {
//        return procId;
//    }
//
//    @Override
//    public void startRequest(OSDRequest rq) {
//        //don't need to do anything, all done in proc stage!
//        rq.sendSuccess(response);
//    }
//
//    @Override
//    public void startInternalEvent(Object[] args) {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }
//
//    @Override
//    public Serializable parseRPCMessage(ReusableBuffer data, OSDRequest rq) throws Exception {
//        keep_file_openRequest rpcrq = new keep_file_openRequest();
//        rpcrq.deserialize(data);
//
//        rq.setFileId(rpcrq.getFile_id());
//        rq.setCapability(new Capability(rpcrq.getCredentials().getXcap(),sharedSecret));
//        rq.setLocationList(new XLocations(rpcrq.getCredentials().getXlocs(), localUUID));
//
//        return rpcrq;
//    }
//
//    @Override
//    public boolean requiresCapability() {
//        return true;
//    }
//
//}
