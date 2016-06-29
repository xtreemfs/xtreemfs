/*
 * Copyright (c) 2008-2011 by Paul Seiferth, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.util.ArrayList;
import java.util.Collection;

import org.xtreemfs.common.libxtreemfs.exceptions.UUIDIteratorListIsEmpyException;

/**
 * Stores a list of all UUIDs of a replicated service and allows to iterate through them.
 * 
 * If an UUID was marked as failed and this is the current UUID, the next call of GetUUID() will return
 * another available, not as failed marked, UUID.
 * 
 * If the last UUID in the list is marked as failed, the status of all entries will be reset and the current
 * UUID is set to the first in the list.
 * 
 * Additionally, it is allowed to set the current UUID to a specific one, regardless of its current state.
 * This is needed in case a service did redirect a request to another UUID.
 */
public class UUIDIterator {

    static private class UUIDItem {
        public String  uuid;

        /**
         * Shows whether this UUID has failed
         */
        public boolean markedAsFailed;

        /**
         * Represents a UUID
         */
        public UUIDItem(String addUUID) {
            markedAsFailed = false;
            uuid = addUUID;
        }
    }

    private ArrayList<UUIDItem> uuids;
    private UUIDItem            currentUUID;

    /**
     * Creates a new instance of UUIDIterator with an empty UUID list.
     */
    public UUIDIterator() {
        uuids = new ArrayList<UUIDIterator.UUIDItem>();
        currentUUID = null;
    }
    
    /**
     * Creates a new instance of UUIDIterator with an UUID list containing all the UUIDs in the specified collection.
     */
    public UUIDIterator(Collection<String> uuids) {
        this();
        addUUIDs(uuids);
    }

    /**
     * Appends "uuid" to the list of UUIDs. Does not change the current UUID.
     */
    public synchronized void addUUID(String uuid) {
        UUIDItem entry = new UUIDItem(uuid);

        uuids.add(entry);

        if (uuids.size() == 1) {
            currentUUID = entry;
        }
    }
    
    /**
     * Appends all the UUIDs in the specified collection to list of UUIDs. Does not change the current UUID.
     */
    public synchronized void addUUIDs(Collection<String> uuids) {
        for (String uuid : uuids) {
            addUUID(uuid);
        }
    }

    /**
     * Clears the list of UUIDs.
     */
    public synchronized void clear() {
        uuids.clear();
        currentUUID = null;
    }

    /**
     * Atomically clears the list and adds "uuid" to avoid an empty list.
     */
    public synchronized void clearAndAddUUID(String uuid) {
        this.clear();
        this.addUUID(uuid);
    }

    /**
     * Atomically clears the list and adds all the UUIDs in the specified collection.
     */
    public synchronized void clearAndAddUUIDs(Collection<String> uuids) {
        this.clear();
        this.addUUIDs(uuids);
    }

    /**
     * Returns the list of UUIDs and their status.
     */
    public synchronized String debugString() {
        StringBuffer debugStringBuffer = new StringBuffer("[ ");

        for (UUIDItem item : uuids) {
            debugStringBuffer.append("[ " + item.uuid + ", " + item.markedAsFailed + " ]");
        }

        debugStringBuffer.append(" ]");

        return debugStringBuffer.toString();
    }

    /**
     * Get the current UUID (by default the first in the list).
     **/
    public synchronized String getUUID() throws UUIDIteratorListIsEmpyException {
        if (uuids.isEmpty()) {
            throw new UUIDIteratorListIsEmpyException("GetUUID() failed as no current "
                    + " UUID is set. Size of list of UUIDs: " + uuids.size());
        } else {
            assert (!currentUUID.markedAsFailed);
            return currentUUID.uuid;

        }
    }

    /**
     * Marks "uuid" as failed. Use this function to advance to the next in the list.
     */
    public synchronized void markUUIDAsFailed(String uuid) {
        // Only do something if currentUUID is uuid
        if (currentUUID != null && currentUUID.uuid.equals(uuid)) {
            currentUUID.markedAsFailed = true;

            int index = uuids.indexOf(currentUUID);
            // if this is the last UUID in the list, revert all
            if (index == (uuids.size() - 1)) {
                for (UUIDItem item : uuids) {
                    item.markedAsFailed = false;
                }
                currentUUID = uuids.get(0);
            } else { // set currenUUID to the following UUID
                currentUUID = uuids.get(index + 1);
            }
        }
    }

    /**
     * Sets "uuid" as current UUID. If uuid was not found in the list of UUIDs, it will be added to the
     * UUIDIterator.
     */
    public synchronized void setCurrentUUID(String uuid) {
        // Search "uuid" in "uuids_" and set it to the current UUID.
        for (UUIDItem item : uuids) {
            if (item.uuid.equals(uuid)) {
                currentUUID = item;
                return;
            }
        }

        // UUID was not found, add it.
        UUIDItem entry = new UUIDItem(uuid);
        uuids.add(entry);
        currentUUID = entry;
        return;
    }
    
    /**
     * Get the number of UUIDs in this iterator regardless if they are marked as failed or not.
     * 
     * @return int
     *          Number of UUIDs.
     */
    public synchronized int size() {
        return uuids.size();
    }

}
