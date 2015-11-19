/*
 * Copyright (c) 2011 by Paul Seiferth,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.drain;

import java.util.List;

import org.xtreemfs.osd.drain.OSDDrain.FileInformation;

/**
 * @author bzcseife
 *
 * <br>Mar 31, 2011
 */

public class OSDDrainException extends Exception {


    
    public enum ErrorState {
        INITIALIZATION,
        SET_SERVICE_STATUS,
        GET_FILE_LIST,
        UPDATE_MRC_ADDRESSES,
        REMOVE_NON_EXISTING_IDS,
        GET_REPLICA_INFO,
        DRAIN_COORDINATED,
        SET_UPDATE_POLICY,
        SET_RONLY,
        CREATE_REPLICAS,
        START_REPLICATION,
        WAIT_FOR_REPLICATION,
        REMOVE_REPLICAS,
        UNSET_RONLY,
        UNSET_UPDATE_POLICY,
        DELETE_FILES,
        SHUTDOWN_OSD,
        WAIT_FOR_XLOCSET_INSTALLATION
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * List of FileInfos from all files that has to be moved to another OSD to remove this OSD.
     * For these files the operations which are done the ErrorState has to be reverted.
     */
    private List<FileInformation> fileInfosAll;
    
    /**
     * List of FileInfos from files that where correctly processed in the current step before the
     * error occurred. For these files the operation which is done in the ErrorState has to be reverted.  
     */
    private List<FileInformation> fileInfosCurrent;
    private ErrorState errorState;
    

    public void setErrorState(ErrorState errorState) {
        this.errorState = errorState;
    }

    public OSDDrainException(String message, ErrorState errorState) {
        super(message);
        this.errorState = errorState; 
        this.fileInfosAll = null;
        this.fileInfosCurrent = null;
        
    };
    
    public OSDDrainException(String message, ErrorState errorState, List<FileInformation> fileInfosAll) {
        super(message);
        this.fileInfosAll = fileInfosAll;
        this.fileInfosCurrent = null;
    };
    
    public OSDDrainException(String message, ErrorState errorState, List<FileInformation> fileInfosAll, 
            List<FileInformation> fileInfosCurrent) {
        super(message);
        this.errorState = errorState;
        this.fileInfosAll = fileInfosAll;
        this.fileInfosCurrent = fileInfosCurrent;
    }
  
    public List<FileInformation> getFileInfosAll() {
        return fileInfosAll;
    }

    public void setFileInfosAll(List<FileInformation> fileInfosAll) {
        this.fileInfosAll = fileInfosAll;
    }

    public List<FileInformation> getFileInfosCurrent() {
        return fileInfosCurrent;
    }

    public void setFileInfosCurrent(List<FileInformation> fileInfosCurrent) {
        this.fileInfosCurrent = fileInfosCurrent;
    }

    public ErrorState getErrorState() {
        return errorState;
    }
}
