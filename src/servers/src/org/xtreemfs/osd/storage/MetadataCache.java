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
 * AUTHORS: Eugenio Cesario (CNR), Björn Kolbeck (ZIB), Christian Lorenz (ZIB),
 * Jan Stender (ZIB)
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
