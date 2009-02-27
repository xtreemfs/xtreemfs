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
 * AUTHORS: Nele Andersen (ZIB)
 */
package org.xtreemfs.common.clients.scrubber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class FileState{
    
    private enum ObjectState {TODO, READING, DONE;}
    
    private List<ObjectState> objectStates;
    private int NoOfObjectsEstimate = Integer.MAX_VALUE;
    private long stripeSize;
    private long fileSize = -1;
    private boolean fileDone = false;
    
    /**
     * 
     * @param stripeSize
     * @param size - the file size stored in the meta data
     */
    public FileState(long stripeSize, int size) {
        this.stripeSize = stripeSize;
        objectStates = Collections.synchronizedList(
                new ArrayList<ObjectState>(size));
        for(int i = 0; i < size; i++)
            objectStates.add(ObjectState.TODO);
    }

    public boolean isFileDone() { return fileDone; }
    
    /**
     * 
     * @return returns the file size read if EOF has been read, otherwise -1 is returned.
     */
    public long getFileSize() {
        if(fileDone && fileSize == -1)
            fileSize = NoOfObjectsEstimate * stripeSize;
        return fileSize;
    }

    /**
     * Called by Multispeedy or Main thread.
     * Changes the state of the object specified by the parameter objectNo to 
     * DONE. If the file is not marked as unreadable and EOF has been read and
     * all objects are DONE, the file is marked as done. 
     */
    public void incorporateReadResult(int objectNo, long bytesRead) {
        assert objectStates.get(objectNo).equals(ObjectState.READING);
        objectStates.set(objectNo, ObjectState.DONE);
        if(bytesRead > 0) { // some data read
            assert NoOfObjectsEstimate >= objectNo;
            if(bytesRead != stripeSize) {
                NoOfObjectsEstimate = objectNo;
                fileSize = objectNo * stripeSize + bytesRead;
            }
        }
        else { // read of object after after EOF
            NoOfObjectsEstimate = Math.min(NoOfObjectsEstimate,objectNo);
        }
        
        // check if file is finished and update flag
        if(NoOfObjectsEstimate != Integer.MAX_VALUE){
            fileDone = true;
            for(int i = 0; i <= NoOfObjectsEstimate; i++){
                if(!objectStates.get(i).equals(ObjectState.DONE)){
                    fileDone = false;
                    break;
                }
            }
        }
        // if the object is the last object and the file is not
        // done, the file was longer as expected, and another object
        // is added to the object states.
        else if((objectStates.size()-1 == objectNo)){
            addObject();
        }
    }

    /**
     * Called by Multispeedy or Main thread.
     * Sets the object state to READING
     * @param objectNo
     */
    public void markObjectAsInFlight(int objectNo){
        objectStates.set(objectNo, ObjectState.READING);
    }
    
    /**
     * 
     * @param objectNo
     * @return returns true if the object state of the object specified by 
     * objectNo is TODO, returns false otherwise.
     */
    public boolean isTodo(int objectNo) {
        return objectStates.get(objectNo).equals(ObjectState.TODO);
    }
    

    private void addObject() {
        objectStates.add(ObjectState.TODO);
    }
    
    public void setObjectState(int objectNo, ObjectState state){
        objectStates.set(objectNo, state);
    }
    
    public List<ObjectState> getObjectStates(){
        return objectStates;
    }
    
    public int getNoOfObjectStates(){
        return objectStates.size();
    }
}
