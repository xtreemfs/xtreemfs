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
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.new_mrc.ErrorRecord;
import org.xtreemfs.new_mrc.MRCRequest;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.UserException;
import org.xtreemfs.new_mrc.ErrorRecord.ErrorClass;
import org.xtreemfs.new_mrc.database.AtomicDBUpdate;
import org.xtreemfs.new_mrc.database.DBAdminTool;
import org.xtreemfs.new_mrc.database.DatabaseException;
import org.xtreemfs.new_mrc.database.StorageManager;
import org.xtreemfs.new_mrc.database.DBAdminTool.DBRestoreState;
import org.xtreemfs.new_mrc.volumes.VolumeManager;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

/**
 * 
 * @author stender
 */
public class RestoreDBOperation extends MRCOperation {
    
    static class Args {
        public String dumpFile;
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
            
            // First, check if any volume exists already. If so, deny the
            // operation for security reasons.
            if (vMan.getVolumes().size() != 0)
                throw new UserException(
                    "Restoring from a dump is only possible on an MRC with no database. Please delete the existing MRC database on the server and restart the MRC!");
            
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(new File(rqArgs.dumpFile), new DefaultHandler() {
                
                private DBRestoreState state;
                
                private int            dbVersion = 1;
                
                public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                    
                    try {
                        
                        if (qName.equals("volume")) {
                            
                            final String id = attributes.getValue(attributes.getIndex("id"));
                            final String name = attributes.getValue(attributes.getIndex("name"));
                            final short acPol = Short.parseShort(attributes.getValue(attributes
                                    .getIndex("acPolicy")));
                            final short osdPol = Short.parseShort(attributes.getValue(attributes
                                    .getIndex("osdPolicy")));
                            final String osdPolArgs = attributes.getIndex("osdPolicyArgs") == -1 ? null
                                : attributes.getValue(attributes.getIndex("osdPolicyArgs"));
                            
                            state = new DBRestoreState();
                            state.currentVolume = new VolumeInfo() {
                                
                                public short getAcPolicyId() {
                                    return acPol;
                                }
                                
                                public String getId() {
                                    return id;
                                }
                                
                                public String getName() {
                                    return name;
                                }
                                
                                public String getOsdPolicyArgs() {
                                    return osdPolArgs;
                                }
                                
                                public short getOsdPolicyId() {
                                    return osdPol;
                                }
                                
                                public void setOsdPolicyArgs(String osdPolicyArgs) {
                                }
                                
                                public void setOsdPolicyId(short osdPolicyId) {
                                }
                                
                            };
                            
                        }

                        else if (qName.equals("filesystem"))
                            try {
                                dbVersion = Integer.parseInt(attributes.getValue(attributes
                                        .getIndex("dbversion")));
                            } catch (Exception exc) {
                                Logging.logMessage(Logging.LEVEL_WARN, this,
                                    "restoring database with invalid version number");
                            }
                        
                        else
                            handleNestedElement(qName, attributes, true);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "could not restore DB from XML dump");
                        Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                        throw new SAXException(exc);
                    }
                }
                
                public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                    
                    try {
                        if (qName.equals("slice") || qName.equals("filesystem"))
                            return;
                        
                        handleNestedElement(qName, null, false);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_ERROR, this,
                            "could not restore DB from XML dump");
                        Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
                        throw new SAXException(exc);
                    }
                }
                
                private void handleNestedElement(String qName, Attributes attributes,
                    boolean openTag) throws UserException, DatabaseException {
                    
                    if (qName.equals("volume")) {
                        
                        // set the largest file ID
                        StorageManager sMan = vMan.getStorageManager(state.currentVolume.getId());
                        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
                        sMan.setLastFileId(state.largestFileId, update);
                        update.execute();
                        state.largestFileId = 0;
                        
                    } else if (qName.equals("dir"))
                        DBAdminTool.restoreDir(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("file"))
                        DBAdminTool.restoreFile(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("xLocList"))
                        DBAdminTool.restoreXLocList(vMan, master.getFileAccessManager(),
                            attributes, state, dbVersion, openTag);
                    else if (qName.equals("xLoc"))
                        DBAdminTool.restoreXLoc(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("osd"))
                        DBAdminTool.restoreOSD(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("acl"))
                        DBAdminTool.restoreACL(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("entry"))
                        DBAdminTool.restoreEntry(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                    else if (qName.equals("attr"))
                        DBAdminTool.restoreAttr(vMan, master.getFileAccessManager(), attributes,
                            state, dbVersion, openTag);
                }
                
            });
            
            finishRequest(rq);
            
        } catch (UserException exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getMessage(), exc));
        } catch (SAXException exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.USER_EXCEPTION, exc.getException()
                    .getMessage() == null ? "an error has occured" : exc.getException()
                    .getMessage(), exc.getException()));
        } catch (Exception exc) {
            finishRequest(rq, new ErrorRecord(ErrorClass.INTERNAL_SERVER_ERROR,
                exc.getMessage() == null ? "an error has occurred" : exc.getMessage(), exc));
        }
    }
    
    @Override
    public ErrorRecord parseRPCBody(MRCRequest rq, List<Object> arguments) {
        
        Args args = new Args();
        
        try {
            
            args.dumpFile = (String) arguments.get(0);
            
            if (arguments.size() == 1)
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
    
}
