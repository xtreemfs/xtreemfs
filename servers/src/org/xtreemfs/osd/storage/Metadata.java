/*  Copyright (c) 2008 Consiglio Nazionale delle Ricerche and
    Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Eugenio Cesario (CNR), Björn Kolbeck (ZIB), Christian Lorenz (ZIB)
 */

package org.xtreemfs.osd.storage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** This class stores the metadata of every file stored in the OSDs
 *
 *  @author Jesus Malo (jmalo)
 */
public class Metadata implements java.io.Serializable {
    
    private long knownSize = 0;    
    private String file;
    
    /** Creates a new instance of Metadata
     *  @param f File name where the object is stored
     */
    public Metadata(String f) {
        file = f;
    }
 
    /**	It stores the object in a file
     */
    public void store() throws IOException {
        ObjectOutputStream warehouse = new ObjectOutputStream(new FileOutputStream(file));

        warehouse.writeObject(this);	
        
        warehouse.close();
    }
    
    /**	It retrieves the object stored in a file
     *	@return The object stored in the file
     */
    public Metadata retrieve() throws IOException {
        ObjectInputStream warehouse = new ObjectInputStream(new FileInputStream(file));

        Object obj;
        
        try {
            obj = warehouse.readObject();
        } catch (ClassNotFoundException ex) {
            throw new IOException(ex.getMessage());
        }
        
        warehouse.close();

        return (Metadata) obj;
    }    
    
    /**	It gets the latest known size of a file
     *	@return The latest known size of a file
     */    
    public long getKnownSize() throws IOException {
        try {
            Metadata metadata = retrieve();
            return metadata.knownSize;
        }
        catch (FileNotFoundException e) {
            return 0;		
        }    
    }
    
    /**	It sets the known size of a file
     *	@param file The file whose known size will be set 
     *	@param newSize The new known size of the file
     */
    public void putKnownSize(long newSize) throws IOException {

        assert newSize >= 0 : "newSize = " + newSize;

        Metadata metadata;

        try {
            metadata = retrieve();
        }
        catch(FileNotFoundException e) {
            metadata = new Metadata(file);
        }

        metadata.knownSize = newSize;
        metadata.store();
    }    

}
