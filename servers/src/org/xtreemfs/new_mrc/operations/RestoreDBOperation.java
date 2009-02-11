/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.new_mrc.operations;

import java.io.File;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.mrc.brain.storage.SliceID;
import org.xtreemfs.mrc.brain.storage.StorageManager.RestoreState;
import org.xtreemfs.mrc.slices.SliceInfo;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.ac.FileAccessManager;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.metadata.FileMetadata;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class RestoreDBOperation extends MRCOperation {
    
    static class Args {
        
        public String path;
        
        public String userId;
        
        public String groupId;
        
    }
    
    public static final String RPC_NAME = ".restoredb";
    
    public RestoreDBOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public boolean hasArguments() {
        return true;
    }
    
    @Override
    public boolean isAuthRequired() {
        return true;
    }
    
    @Override
    public void startRequest(MRCRequest rq) {
        
        try {
            
            Args rqArgs = (Args) rq.getRequestArgs();
            
            final VolumeManager vMan = master.getVolumeManager();
            final FileAccessManager faMan = master.getFileAccessManager();
            
            Path p = new Path(rqArgs.path);
            
            VolumeInfo volume = vMan.getVolumeByName(p.getComp(0));
            StorageManager sMan = vMan.getStorageManager(volume.getId());
            PathResolver res = new PathResolver(sMan, p);
            
            // check whether the path prefix is searchable
            faMan.checkSearchPermission(sMan, res, rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            // check whether file exists
            res.checkIfFileDoesNotExist();
            
            FileMetadata file = res.getFile();
            
            // if the file refers to a symbolic link, resolve the link
            String target = sMan.getSoftlinkTarget(file.getId());
            if (target != null) {
                rqArgs.path = target;
                p = new Path(rqArgs.path);
                
                // if the local MRC is not responsible, send a redirect
                if (!vMan.hasVolume(p.getComp(0))) {
                    finishRequest(rq, new ErrorRecord(ErrorClass.REDIRECT, target));
                    return;
                }
                
                volume = vMan.getVolumeByName(p.getComp(0));
                sMan = vMan.getStorageManager(volume.getId());
                res = new PathResolver(sMan, p);
                file = res.getFile();
            }
            
            // check whether the owner may be changed
            faMan.checkPrivilegedPermissions(sMan, file, rq.getDetails().userId,
                rq.getDetails().superUser, rq.getDetails().groupIds);
            
            AtomicDBUpdate update = sMan.createAtomicDBUpdate(master, rq);
            
            // change owner and owning group
            file.setOwnerAndGroup(rqArgs.userId == null ? file.getOwnerId() : rqArgs.userId,
                rqArgs.groupId == null ? file.getOwningGroupId() : rqArgs.groupId);
            sMan.setMetadata(file, FileMetadata.RC_METADATA, update);
            
            // update POSIX timestamps
            MRCOpHelper.updateFileTimes(res.getParentDirId(), file, false, true, false, sMan,
                update);
            
            // FIXME: this line is needed due to a BUG in the client which
            // expects some useless return value
            rq.setData(ReusableBuffer.wrap(JSONParser.writeJSON(null).getBytes()));
            
            update.execute();
            
        } catch (UserException exc) {
            Logging.logMessage(Logging.LEVEL_TRACE, this, exc);
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getErrno(), exc
                    .getMessage(), exc));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                "an error has occurred", exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.path = (String) arguments.get(0);
            args.userId = (String) arguments.get(1);
            args.groupId = (String) arguments.get(2);
            
            if (arguments.size() == 3)
                return null;
            
            throw new Exception();
            
        } catch (Exception exc) {
            try {
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "': " + JSONParser.writeJSON(arguments));
            } catch (JSONException je) {
                Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                return new ErrorRecord(ErrorClass.BAD_REQUEST, "invalid arguments for operation '"
                    + getClass().getSimpleName() + "'");
            }
        } finally {
            rq.setRequestArgs(args);
        }
    }
    
//    public void restoreDBFromDump(String dumpFilePath) throws Exception {
//        
//        // First, check if any volume exists already. If so, deny the operation
//        // for security reasons.
//        if (!volumesById.isEmpty())
//            throw new Exception(
//                "Restoring from a dump is only possible on an MRC with no database. Please delete the existing MRC database on the server and restart the MRC!");
//        
//        SAXParserFactory spf = SAXParserFactory.newInstance();
//        SAXParser sp = spf.newSAXParser();
//        sp.parse(new File(dumpFilePath), new DefaultHandler() {
//            
//            private StorageManager sMan;
//            
//            private RestoreState   state;
//            
//            private int            dbVersion = 1;
//            
//            public void startElement(String uri, String localName, String qName,
//                Attributes attributes) throws SAXException {
//                
//                try {
//                    
//                    if (qName.equals("volume")) {
//                        String id = attributes.getValue(attributes.getIndex("id"));
//                        String name = attributes.getValue(attributes.getIndex("name"));
//                        long acPol = Long.parseLong(attributes.getValue(attributes
//                                .getIndex("acPolicy")));
//                        long osdPol = Long.parseLong(attributes.getValue(attributes
//                                .getIndex("osdPolicy")));
//                        long partPol = Long.parseLong(attributes.getValue(attributes
//                                .getIndex("partPolicy")));
//                        String osdPolArgs = attributes.getIndex("osdPolicyArgs") == -1 ? null
//                            : attributes.getValue(attributes.getIndex("osdPolicyArgs"));
//                        
//                        createVolume(id, name, acPol, osdPol, osdPolArgs, partPol, true, false);
//                    }
//
//                    else if (qName.equals("slice")) {
//                        SliceID id = new SliceID(attributes.getValue(attributes.getIndex("id")));
//                        
//                        createSlice(new SliceInfo(id, null), false);
//                        
//                        sMan = getSliceDB(id, '*');
//                        state = new StorageManager.RestoreState();
//                    }
//
//                    else if (qName.equals("filesystem"))
//                        try {
//                            dbVersion = Integer.parseInt(attributes.getValue(attributes
//                                    .getIndex("dbversion")));
//                        } catch (Exception exc) {
//                            Logging.logMessage(Logging.LEVEL_WARN, this,
//                                "restoring database with invalid version number");
//                        }
//                    
//                    else
//                        sMan.restoreDBFromDump(qName, attributes, state, true, dbVersion);
//                    
//                } catch (Exception exc) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this,
//                        "could not restore DB from XML dump: " + exc);
//                }
//            }
//            
//            public void endElement(String uri, String localName, String qName) throws SAXException {
//                
//                try {
//                    if (qName.equals("volume") || qName.equals("slice")
//                        || qName.equals("filesystem"))
//                        return;
//                    
//                    sMan.restoreDBFromDump(qName, null, state, false, dbVersion);
//                } catch (Exception exc) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this,
//                        "could not restore DB from XML dump");
//                    Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
//                }
//            }
//            
//            public void endDocument() throws SAXException {
//                try {
//                    compactDB();
//                    completeDBCompaction();
//                } catch (Exception exc) {
//                    Logging.logMessage(Logging.LEVEL_ERROR, this,
//                        "could not restore DB from XML dump");
//                    Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
//                }
//            }
//            
//        });
//    }
    
}
