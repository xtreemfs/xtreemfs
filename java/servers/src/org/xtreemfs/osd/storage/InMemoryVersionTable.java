/*
 * Copyright (c) 2014 by Johannes Dillmann, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.storage;

import java.io.IOException;
import java.util.SortedMap;

/**
 * InMemoryVersionTable is intended for use with {@link InMemoryStorageLayout}. It will share its verstionTable object
 * with it and is only updating references.
 */
public class InMemoryVersionTable extends VersionTable {
    private final InMemoryStorageLayout layout;
    private final String                fileId;

    public InMemoryVersionTable(String fileId, InMemoryStorageLayout layout) {
        super();
        this.fileId = fileId;
        this.layout = layout;
    }

    @Override
    protected void doLoad() throws IOException {
        SortedMap<Long, Version> versionTable = layout.getVersionTable(fileId);

        if (versionTable == null) {
            // If no versionTable is stored, the current one is just cleared.
            vt.clear();
        } else {
            // If a table exists, it is used in the InMemoryVersionTable.
            vt = versionTable;
        }
    }

    @Override
    protected void doSave() throws IOException {
        // Set the stored versionTable.
        layout.setVersionTable(fileId, vt);
    }

}
