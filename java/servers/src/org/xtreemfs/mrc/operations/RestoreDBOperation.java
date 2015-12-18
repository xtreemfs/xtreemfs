/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.operations;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.foundation.logging.Logging.Category;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.mrc.ErrorRecord;
import org.xtreemfs.mrc.MRCRequest;
import org.xtreemfs.mrc.MRCRequestDispatcher;
import org.xtreemfs.mrc.UserException;
import org.xtreemfs.mrc.database.AtomicDBUpdate;
import org.xtreemfs.mrc.database.DatabaseException;
import org.xtreemfs.mrc.database.StorageManager;
import org.xtreemfs.mrc.database.VolumeManager;
import org.xtreemfs.mrc.utils.DBAdminHelper;
import org.xtreemfs.mrc.utils.DBAdminHelper.DBRestoreState;
import org.xtreemfs.pbrpc.generatedinterfaces.Common.emptyResponse;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.xtreemfs_dump_restore_databaseRequest;

/**
 * 
 * @author stender
 */
public class RestoreDBOperation extends MRCOperation {
    
    public RestoreDBOperation(MRCRequestDispatcher master) {
        super(master);
    }
    
    @Override
    public void startRequest(MRCRequest rq) throws Throwable {
        
        try {
            
            final xtreemfs_dump_restore_databaseRequest rqArgs = (xtreemfs_dump_restore_databaseRequest) rq
                    .getRequestArgs();
            
            // check password to ensure that user is authorized
            if (master.getConfig().getAdminPassword().length() > 0
                && !master.getConfig().getAdminPassword().equals(rq.getDetails().password))
                throw new UserException(POSIXErrno.POSIX_ERROR_EPERM, "invalid password");
            
            final VolumeManager vMan = master.getVolumeManager();
            
            if (vMan.getStorageManagers() == null)
                throw new UserException(POSIXErrno.POSIX_ERROR_EINVAL,
                        "cannot restore database because volume manager has not yet been initialized");
            
            // First, check if any volume exists already. If so, deny the
            // operation for security reasons.
            if (vMan.getStorageManagers().size() != 0)
                throw new UserException(
                    POSIXErrno.POSIX_ERROR_EPERM,
                    "Restoring from a dump is only possible on an MRC with no database. Please delete the existing MRC database on the server and restart the MRC!");
            
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(new File(rqArgs.getDumpFile()), new DefaultHandler() {
                
                private DBRestoreState state;
                
                private int            dbVersion = 1;
                
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                    
                    try {
                        
                        if (qName.equals("volume")) {
                            
                            state = new DBRestoreState();
                            state.currentVolumeId = attributes.getValue(attributes.getIndex("id"));
                            state.currentVolumeName = attributes.getValue(attributes.getIndex("name"));
                            state.currentVolumeACPolicy = Short.parseShort(attributes.getValue(attributes
                                    .getIndex("acPolicy")));
                            if (attributes.getIndex("quota") == -1) { // fallback for old xtreemfs versions
                                state.currentVolumeQuota = QuotaConstants.UNLIMITED_QUOTA;
                            } else {
                                state.currentVolumeQuota = Long.valueOf(attributes.getValue(attributes
                                        .getIndex("quota")));
                            }
                        }

                        else if (qName.equals("filesystem"))
                            try {
                                dbVersion = Integer.parseInt(attributes.getValue(attributes
                                        .getIndex("dbversion")));
                            } catch (Exception exc) {
                                Logging.logMessage(Logging.LEVEL_WARN, Category.storage, this,
                                    "restoring database with invalid version number");
                            }
                        
                        else
                            handleNestedElement(qName, attributes, true);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                            "could not restore DB from XML dump");
                        Logging.logUserError(Logging.LEVEL_ERROR, Category.storage, this, exc);
                        throw new SAXException(exc);
                    }
                }
                
                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    
                    try {
                        if (qName.equals("slice") || qName.equals("filesystem"))
                            return;
                        
                        handleNestedElement(qName, null, false);
                        
                    } catch (Exception exc) {
                        Logging.logMessage(Logging.LEVEL_ERROR, Category.storage, this,
                            "could not restore DB from XML dump");
                        Logging.logUserError(Logging.LEVEL_ERROR, Category.storage, this, exc);
                        throw new SAXException(exc);
                    }
                }
                
                private void handleNestedElement(String qName, Attributes attributes, boolean openTag)
                    throws UserException, DatabaseException {
                    
                    if (qName.equalsIgnoreCase("volume")) {
                        
                        // set the largest file ID
                        StorageManager sMan = vMan.getStorageManager(state.currentVolumeId);
                        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
                        sMan.setLastFileId(state.largestFileId, update);
                        update.execute();

                        // reload values from db
                        sMan.getVolumeInfo().reload();

                        state.largestFileId = 0;
                        
                    } else if (qName.equalsIgnoreCase("dir"))
                        DBAdminHelper.restoreDir(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("file"))
                        DBAdminHelper.restoreFile(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("xLocList"))
                        DBAdminHelper.restoreXLocList(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("xLoc"))
                        DBAdminHelper.restoreXLoc(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("osd"))
                        DBAdminHelper.restoreOSD(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("acl"))
                        DBAdminHelper.restoreACL(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("entry"))
                        DBAdminHelper.restoreEntry(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("attr"))
                        DBAdminHelper.restoreAttr(vMan, master.getFileAccessManager(), attributes, state,
                            dbVersion, openTag);
                    else if (qName.equalsIgnoreCase("quota"))
                        DBAdminHelper.restoreQuota(vMan, master.getFileAccessManager(), attributes, state, 
                            dbVersion, openTag);
                }
                
            });
            
            // set the response
            rq.setResponse(emptyResponse.getDefaultInstance());
            finishRequest(rq);
            
        } catch (SAXException exc) {
            finishRequest(rq, new ErrorRecord(ErrorType.ERRNO, POSIXErrno.POSIX_ERROR_NONE, exc
                    .getException().getMessage() == null ? "an error has occurred" : exc.getException()
                    .getMessage(), exc.getException()));
        }
    }
    
}
