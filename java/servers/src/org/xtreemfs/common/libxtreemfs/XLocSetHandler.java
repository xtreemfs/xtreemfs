/*
 * Copyright (c) 2013 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.libxtreemfs;

import java.io.IOException;

import org.xtreemfs.common.libxtreemfs.exceptions.InternalServerErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.FileCredentials;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.XLocSet;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;


/**
 * The XLocSetHandler is used to renew the xLocSet and update request {@link Message}s together with
 * {@link UUIDIterator}s if an OSD returns an InvalidViewException. It is called by {@link RPCCaller#syncCall}, which
 * will then use the updated request Message and the UUIDIterator for subsequent requests.<br>
 * <br>
 * The default implementation does update the requests if is an instance of {@link Message} and a field named
 * "file_credentials" exists.<br>
 * It does not change the UUIDIterator because the default UUIDIterator, stored in {@link FileInfo}, will be already
 * updates when the new xLocSet is updated in {@link FileInfo#updateXLocSetAndRest}. If striping is enabled, the update
 * function has to be overwritten.
 * 
 */
public class XLocSetHandler<C> {

    final FileHandleImplementation fileHandle;
    final FileInfo                 fileInfo;

    public XLocSetHandler(FileHandleImplementation fileHandle, FileInfo fileInfo) {
        this.fileHandle = fileHandle;
        this.fileInfo = fileInfo;
    }

    /**
     * Renews the xLocSet and stores it for every fileHandle instance with the same fileId. <br>
     * This will block until a new xLocSet is loaded.
     * 
     * @throws PosixErrorException
     * @throws InternalServerErrorException
     * @throws IOException
     */
    public void renew() throws PosixErrorException, InternalServerErrorException, IOException {
        fileInfo.renewXLocSet(fileHandle);
    }

    /**
     * Gets the current xLocSet. <br>
     * It is the same for every fileHandle instance with the same fileId.
     * 
     * @return
     */
    public XLocSet getXLocSet() {
        return fileInfo.getXLocSet();
    }

    /**
     * Since the default UUIDIterator is already updated in {@link FileInfo#updateXLocSetAndRest(XLocSet, boolean)} the
     * currentUUIDIterator is just returned unmodified.
     * 
     * @param currentUUIDIterator
     * @return
     */
    public UUIDIterator updateUUIDIterator(UUIDIterator currentUUIDIterator) {
        return currentUUIDIterator;
    }

    /**
     * Looks for a field by the name "file_credentials" of type FileCredentials and updates it's xLocSet.
     * 
     * @param request
     * @return the request containing the new xLocSet or the original unmodified one.
     */
    @SuppressWarnings("unchecked")
    public C updateXLocSetInRequest(C request) {
        // Only requests that are protobuf Messages can be updated.
        if (request instanceof Message) {
            Message requestMessage = (Message) request;
            FieldDescriptor fd = requestMessage.getDescriptorForType().findFieldByName("file_credentials");

            if (fd != null && requestMessage.hasField(fd) && requestMessage.getField(fd) instanceof FileCredentials) {
                FileCredentials.Builder fcBuilder = ((FileCredentials) requestMessage.getField(fd)).toBuilder();
                fcBuilder.setXlocs(getXLocSet());

                Builder requestBuilder = requestMessage.toBuilder();
                requestBuilder.setField(fd, fcBuilder.buildPartial());

                return (C) requestBuilder.buildPartial();
            }
        }

        return request;
    }
}
