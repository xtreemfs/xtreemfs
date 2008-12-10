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

/**
 * Interface for accessing an Access Control List. Access Control Lists may be
 * assigned to files and directories.
 */
public interface ACL extends Metadata {
    
    /**
     * Represents an ACL entry.
     */
    public static interface Entry {
        
        public String getEntity();
        
        public int getRights();
    }
    
    /**
     * Returns an iterator for all entries.
     * 
     * @return an iterator
     */
    public Iterator<Entry> iterator();
    
    /**
     * Returns the number of entries stored in the ACL.
     * 
     * @return the number of entries
     */
    public int getEntryCount();
    
    /**
     * Returns the access rights associated with the entity.
     * 
     * @param entity
     *            the entity
     * @return the access rights associated with the entity, or
     *         <code>null</code> if the entity does not exist
     */
    public Integer getRights(String entity);
    
    /**
     * Modifies an entry. If the entity does not exist, a new entry will be
     * created.
     * 
     * @param entity
     *            the entity
     * @param rights
     *            the access rights
     */
    public void editEntry(String entity, int rights);
    
    /**
     * Deletes an existing entry. Does nothing if the entity does not exist.
     * 
     * @param entity
     *            the entity of the entry to delete
     */
    public void deleteEntry(String entity);
    
}