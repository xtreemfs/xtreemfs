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

package org.xtreemfs.mrc.brain.metadata;

import java.util.Iterator;

import org.xtreemfs.common.buffer.ASCIIString;

/**
 * Interface for accessing a set of extended attributes. A set of extended
 * attributes may be assigned to a file or directory.
 */
public interface XAttrs extends Metadata {
    
    /**
     * Represents a single extended attribute.
     */
    public static interface Entry {
        
        public String getKey();
        
        public String getValue();
        
        public String getUID();
    }
    
    /**
     * Returns an iterator for all entries.
     * 
     * @return an iterator
     */
    public Iterator<Entry> iterator();
    
    /**
     * Returns the number of stored xattrs.
     * 
     * @return the number of xattrs
     */
    public int getEntryCount();
    
    /**
     * Returns an extended attribute from a given user with a given key.
     * 
     * @param key
     *            the key (name) of the attribute
     * @param uid
     *            the uid of the user
     * @return the value of the attribute, or an empty string if no such
     *         attribute exists
     */
    public ASCIIString getValue(String key, String uid);
    
    /**
     * Modifies an entry. If the key + uid combination does not exist, a new
     * entry will be created.
     * 
     * @param key
     *            the key
     * @param value
     *            the value
     * @param uid
     *            the uid
     */
    public void editEntry(String key, String value, String uid);
    
    /**
     * Deletes an existing entry. Does nothing if the entity does not exist.
     * 
     * @param key
     *            the key of the entry to delete
     */
    public void deleteEntry(String key, String uid);
    
}