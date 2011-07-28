/*
 * Copyright (c) 2009-2011 by Christian Lorenz, Bjoern Kolbeck,
 *                            Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.storage;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;


public class MetadataCache {

    private Map<String, FileMetadata> metadataMap;

    /** Creates a new instance of StorageCache */
    public MetadataCache() {
        metadataMap = new ConcurrentSkipListMap<String, FileMetadata>();
    }

    public FileMetadata getFileInfo(String fileId) {
        assert (fileId != null);
        return metadataMap.get(fileId);
    }

    public void setFileInfo(String fileId, FileMetadata info) {
        assert (info.getFilesize() != 0 || info.getLastObjectNumber() <= 0);
        metadataMap.put(fileId, info);
    }

    public FileMetadata removeFileInfo(String fileId) {
        return metadataMap.remove(fileId);
    }

}
