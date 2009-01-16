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

package org.xtreemfs.new_mrc.volumes;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.babudb.BabuDBInsertGroup;
import org.xtreemfs.babudb.log.DiskLogger.SyncMode;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.mrc.brain.ErrNo;
import org.xtreemfs.mrc.brain.UserException;
import org.xtreemfs.mrc.brain.storage.BackendException;
import org.xtreemfs.mrc.brain.storage.VolIDGen;
import org.xtreemfs.new_mrc.MRCException;
import org.xtreemfs.new_mrc.MRCRequestDispatcher;
import org.xtreemfs.new_mrc.dbaccess.AtomicDBUpdate;
import org.xtreemfs.new_mrc.dbaccess.BabuDBRequestListenerWrapper;
import org.xtreemfs.new_mrc.dbaccess.BabuDBStorageManager;
import org.xtreemfs.new_mrc.dbaccess.DBAccessResultListener;
import org.xtreemfs.new_mrc.dbaccess.DatabaseException;
import org.xtreemfs.new_mrc.dbaccess.StorageManager;
import org.xtreemfs.new_mrc.volumes.metadata.BufferBackedVolumeInfo;
import org.xtreemfs.new_mrc.volumes.metadata.VolumeInfo;

public class BabuDBVolumeManager implements VolumeManager {
    
    private static final String               VOLUME_DB_NAME = "V";
    
    private static final int                  VOL_INDEX      = 0;
    
    private static final int                  VOL_NAME_INDEX = 1;
    
    /** the volume database */
    private BabuDB                            database;
    
    /** the database directory */
    private final String                      dbDir;
    
    /** the database log directory */
    private final String                      dbLogDir;
    
    /** maps the IDs of all locally known volumes to their storage managers */
    private final Map<String, StorageManager> mngrMap;
    
    /**
     * contains all listeners that are notified when volumes are changed
     */
    private final List<VolumeChangeListener>  vcListeners;
    
    public BabuDBVolumeManager(MRCRequestDispatcher master) {
        
        this.vcListeners = new LinkedList<VolumeChangeListener>();
        
        dbDir = master.getConfig().getDbDir();
        dbLogDir = master.getConfig().getDbDir(); // TODO: introduce separate
        // parameter
        mngrMap = new HashMap<String, StorageManager>();
    }
    
    public void init() throws DatabaseException {
        
        try {
            database = new BabuDB(dbDir, dbLogDir, 2, 1024 * 1024 * 16, 5 * 60, SyncMode.FDATASYNC,
                300, 1000);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        try {
            database.createDatabase(VOLUME_DB_NAME, 2);
        } catch (BabuDBException e) {
            // database already exists
            Logging.logMessage(Logging.LEVEL_TRACE, this, "database loaded from '" + dbDir + "'");
            for (VolumeInfo vol : getVolumes())
                mngrMap.put(vol.getId(), new BabuDBStorageManager(database, vol.getName(), vol.getId()));
        }
    }
    
    public void shutdown() {
        database.shutdown();
    }
    
    public VolumeInfo createVolume(String volumeId, String volumeName, short fileAccessPolicyId,
        short osdPolicyId, String osdPolicyArgs, String ownerId, String owningGroupId,
        Map<String, Object> defaultStripingPolicy, DBAccessResultListener listener, Object context)
        throws UserException, DatabaseException, MRCException {
        
        if (volumeName.indexOf('/') != -1 || volumeName.indexOf('\\') != -1)
            throw new UserException(ErrNo.EINVAL, "volume name must not contain '/' or '\\'");
        
        if (hasVolume(volumeName))
            throw new UserException(ErrNo.EEXIST, "volume ' " + volumeName
                + "' already exists locally");
        
        // create the volume
        BufferBackedVolumeInfo volume = new BufferBackedVolumeInfo(volumeId, volumeName,
            fileAccessPolicyId, osdPolicyId, osdPolicyArgs);
        
        // initialize the volume
        short perms = 509; // TODO
        
        // make sure that no volume database with the given name exists
        try {
            database.deleteDatabase(volumeId, true);
        } catch (BabuDBException exc) {
            // ignore
        }
        
        // create the volume database
        try {
            database.createDatabase(volumeId, 5);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        BabuDBStorageManager sMan = new BabuDBStorageManager(database, volumeName, volumeId);
        mngrMap.put(volumeId, sMan);
        
        AtomicDBUpdate update = sMan.createAtomicDBUpdate(null, null);
        sMan.init(ownerId, owningGroupId, perms, defaultStripingPolicy, update);
        update.execute();
        
        try {
            BabuDBInsertGroup ig = database.createInsertGroup(VOLUME_DB_NAME);
            ig.addInsert(VOL_INDEX, volumeId.getBytes(), volume.getBuffer());
            ig.addInsert(VOL_NAME_INDEX, volumeName.getBytes(), volumeId.getBytes());
            database.asyncInsert(ig, new BabuDBRequestListenerWrapper(listener), context);
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
        
        notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, volume);
        
        return volume;
        
    }
    
    public boolean hasVolume(String volumeName) throws DatabaseException {
        try {
            byte[] result = database.syncLookup(VOLUME_DB_NAME, VOL_NAME_INDEX, volumeName
                    .getBytes());
            return result != null;
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public boolean hasVolumeWithId(String volumeId) throws DatabaseException {
        try {
            byte[] result = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX, volumeId.getBytes());
            return result != null;
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public VolumeInfo getVolumeByName(String volumeName) throws DatabaseException, UserException {
        
        try {
            
            byte[] volId = database.syncLookup(VOLUME_DB_NAME, VOL_NAME_INDEX, volumeName
                    .getBytes());
            if (volId == null)
                throw new UserException(ErrNo.ENOENT, "volume '" + volumeName
                    + "' not found on this MRC");
            
            byte[] volumeData = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX, volId);
            
            return new BufferBackedVolumeInfo(volumeData);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public VolumeInfo getVolumeById(String volumeId) throws DatabaseException, UserException {
        
        try {
            
            byte[] volumeData = database.syncLookup(VOLUME_DB_NAME, VOL_INDEX, volumeId.getBytes());
            if (volumeData == null)
                throw new UserException(ErrNo.ENOENT, "volume with id " + volumeId
                    + " not found on this MRC");
            return new BufferBackedVolumeInfo(volumeData);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public List<VolumeInfo> getVolumes() throws DatabaseException {
        
        try {
            
            Iterator<Entry<byte[], byte[]>> it = database.syncPrefixLookup(VOLUME_DB_NAME,
                VOL_INDEX, new byte[0]);
            List<VolumeInfo> list = new LinkedList<VolumeInfo>();
            while (it.hasNext())
                list.add(new BufferBackedVolumeInfo(it.next().getValue()));
            
            return list;
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public void deleteVolume(String volumeName, DBAccessResultListener listener, Object context)
        throws DatabaseException, UserException {
        
        try {
            
            VolumeInfo volume = getVolumeByName(volumeName);
            mngrMap.remove(volume.getId());
            
            BabuDBInsertGroup ig = database.createInsertGroup(VOLUME_DB_NAME);
            ig.addDelete(VOL_INDEX, volume.getId().getBytes());
            ig.addDelete(VOL_NAME_INDEX, volumeName.getBytes());
            
            database.asyncInsert(ig, new BabuDBRequestListenerWrapper(listener), context);
            
        } catch (BabuDBException exc) {
            throw new DatabaseException(exc);
        }
    }
    
    public StorageManager getStorageManager(String volumeId) {
        return mngrMap.get(volumeId);
    }
    
    /**
     * 
     * @param volumeID
     * @param fileID
     * @return true, if the file with the given ID exists, false otherwise.
     * @throws UserException
     *             - if volume does not exist
     * @throws BackendException
     *             - if a backendError occur
     */
    public boolean exists(String volumeID, String fileID) throws UserException, BackendException {
        
        // TODO
        
        // // check the volume - if not available throw UserException
        // VolumeInfo volume = volumesById.get(volumeID);
        // if (volume == null)
        // throw new UserException("could not find volume '" + volumeID);
        //        
        // // get the sliceID - if not available return false.
        // SliceID sliceId = null;
        //        
        // sliceId = partMan.getSlice(volumeID, Long.valueOf(fileID));
        //        
        // // check sliceID for info objects - if not available return false.
        // if (slicesById.get(sliceId) == null)
        // return false;
        //        
        // // get the responsible StorageManager - if not available return
        // false.
        // StorageManager mngr = mngrMap.get(sliceId);
        // if (mngr != null)
        // return mngr.exists(fileID);
        // else
        // return getSliceDB(sliceId, 'r').exists(fileID);
        return false;
    }
    
    public void addVolumeChangeListener(VolumeChangeListener listener) throws IOException,
        DatabaseException {
        
        vcListeners.add(listener);
        
        for (VolumeInfo vol : getVolumes())
            notifyVolumeChangeListeners(VolumeChangeListener.MOD_CHANGED, vol);
    }
    
    public void notifyVolumeChangeListeners(int mod, VolumeInfo vol) {
        for (VolumeChangeListener listener : vcListeners)
            listener.volumeChanged(mod, vol);
    }
    
    @Override
    public String newVolumeId() {
        try {
            return new VolIDGen().getNewVolID();
        } catch (SocketException e) {
            Logging.logMessage(Logging.LEVEL_ERROR, this, e);
            return null;
        }
    }
    
    public void dumpDB(String dumpFilePath) throws Exception {
        
        // TODO
        
        // BufferedWriter xmlWriter = new BufferedWriter(new
        // FileWriter(dumpFilePath));
        // xmlWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        // xmlWriter.write("<filesystem dbversion=\"" +
        // VersionManagement.getMrcDataVersion()
        // + "\">\n");
        //        
        // for (VolumeInfo volume : volumesById.values()) {
        // xmlWriter.write("<volume id=\""
        // + volume.getId()
        // + "\" name=\""
        // + volume.getName()
        // + "\" acPolicy=\""
        // + volume.getAcPolicyId()
        // + "\" osdPolicy=\""
        // + volume.getOsdPolicyId()
        // + (volume.getOsdPolicyArgs() != null ? "\" osdPolicyArgs=\""
        // + volume.getOsdPolicyArgs() : "") + "\">\n");
        //            
        // for (SliceInfo slice : volume.getSlices()) {
        // xmlWriter.write("<slice id=\"" + slice.sliceID + "\">\n");
        // StorageManager sMan = getSliceDB(slice.sliceID, 'r');
        // sMan.dumpDB(xmlWriter);
        // xmlWriter.write("</slice>\n");
        // }
        //            
        // xmlWriter.write("</volume>\n");
        // }
        //        
        // xmlWriter.write("</filesystem>\n");
        // xmlWriter.close();
    }
    
    public void restoreDBFromDump(String dumpFilePath) throws Exception {
        
        // TODO
        
        // First, check if any volume exists already. If so, deny the operation
        // for security reasons.
        // if (!volumesById.isEmpty())
        // throw new Exception(
        // "Restoring from a dump is only possible on an MRC with no database. Please delete the existing MRC database on the server and restart the MRC!"
        // );
        //        
        // SAXParserFactory spf = SAXParserFactory.newInstance();
        // SAXParser sp = spf.newSAXParser();
        // sp.parse(new File(dumpFilePath), new DefaultHandler() {
        //            
        // private StorageManager sMan;
        //            
        // private RestoreState state;
        //            
        // private int dbVersion = 1;
        //            
        // public void startElement(String uri, String localName, String qName,
        // Attributes attributes) throws SAXException {
        //                
        // try {
        //                    
        // if (qName.equals("volume")) {
        // String id = attributes.getValue(attributes.getIndex("id"));
        // String name = attributes.getValue(attributes.getIndex("name"));
        // long acPol = Long.parseLong(attributes.getValue(attributes
        // .getIndex("acPolicy")));
        // long osdPol = Long.parseLong(attributes.getValue(attributes
        // .getIndex("osdPolicy")));
        // long partPol = Long.parseLong(attributes.getValue(attributes
        // .getIndex("partPolicy")));
        // String osdPolArgs = attributes.getIndex("osdPolicyArgs") == -1 ? null
        // : attributes.getValue(attributes.getIndex("osdPolicyArgs"));
        //                        
        // createVolume(id, name, acPol, osdPol, osdPolArgs, partPol, true,
        // false);
        // }
        //
        // else if (qName.equals("slice")) {
        // SliceID id = new
        // SliceID(attributes.getValue(attributes.getIndex("id")));
        //                        
        // createSlice(new SliceInfo(id, null), false);
        //                        
        // sMan = getSliceDB(id, '*');
        // state = new StorageManager.RestoreState();
        // }
        //
        // else if (qName.equals("filesystem"))
        // try {
        // dbVersion = Integer.parseInt(attributes.getValue(attributes
        // .getIndex("dbversion")));
        // } catch (Exception exc) {
        // Logging.logMessage(Logging.LEVEL_WARN, this,
        // "restoring database with invalid version number");
        // }
        //                    
        // else
        // sMan.restoreDBFromDump(qName, attributes, state, true, dbVersion);
        //                    
        // } catch (Exception exc) {
        // Logging.logMessage(Logging.LEVEL_ERROR, this,
        // "could not restore DB from XML dump: " + exc);
        // }
        // }
        //            
        // public void endElement(String uri, String localName, String qName)
        // throws SAXException {
        //                
        // try {
        // if (qName.equals("volume") || qName.equals("slice")
        // || qName.equals("filesystem"))
        // return;
        //                    
        // sMan.restoreDBFromDump(qName, null, state, false, dbVersion);
        // } catch (Exception exc) {
        // Logging.logMessage(Logging.LEVEL_ERROR, this,
        // "could not restore DB from XML dump");
        // Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        // }
        // }
        //            
        // public void endDocument() throws SAXException {
        // try {
        // compactDB();
        // completeDBCompaction();
        // } catch (Exception exc) {
        // Logging.logMessage(Logging.LEVEL_ERROR, this,
        // "could not restore DB from XML dump");
        // Logging.logMessage(Logging.LEVEL_ERROR, this, exc);
        // }
        // }
        //            
        // });
    }
}
