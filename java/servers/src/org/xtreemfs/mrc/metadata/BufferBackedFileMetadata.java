/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedFileMetadata implements FileMetadata {
    
    public static final short      NUM_BUFFERS = 2;
    
    protected static final int     FC_ATIME    = 0;
    
    protected static final int     FC_CTIME    = 4;
    
    protected static final int     FC_MTIME    = 8;
    
    protected static final int     FC_SIZE     = 12;
    
    private final ByteBuffer       fcKeyBuf;
    
    private final ByteBuffer       fcValBuf;
    
    private BufferBackedRCMetadata rcMetadata;
    
    private int                    indexId;
    
    /**
     * Creates a new buffer-backed metadata object from a set of buffers.
     * 
     * @param keyBufs
     *            the database keys
     * @param valBufs
     *            the database values
     * @param indexId
     *            indicates the index from which the key and value buffers were
     *            retrieved
     */
    public BufferBackedFileMetadata(byte[][] keyBufs, byte[][] valBufs, int indexId) {
        
        assert (keyBufs.length == NUM_BUFFERS);
        assert (valBufs.length == NUM_BUFFERS);
        
        byte[][] keyBufsCopy = new byte[keyBufs.length][];
        byte[][] valBufsCopy = new byte[valBufs.length][];
        for (int i = 0; i < keyBufs.length; i++) {
            keyBufsCopy[i] = keyBufs[i] == null ? null : new byte[keyBufs[i].length];
            valBufsCopy[i] = new byte[valBufs[i].length];
            if (keyBufsCopy[i] != null)
                System.arraycopy(keyBufs[i], 0, keyBufsCopy[i], 0, keyBufs[i].length);
            System.arraycopy(valBufs[i], 0, valBufsCopy[i], 0, valBufs[i].length);
        }
        
        // frequently changed metadata
        fcKeyBuf = keyBufsCopy[FC_METADATA] == null ? null : ByteBuffer.wrap(keyBufsCopy[FC_METADATA]);
        fcValBuf = ByteBuffer.wrap(valBufsCopy[FC_METADATA]);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(keyBufsCopy[RC_METADATA], valBufsCopy[RC_METADATA]);
        
        this.indexId = indexId;
    }
    
    /**
     * Creates a new buffer-backed file metadata object.
     * 
     * @param parentId
     * @param fileName
     * @param ownerId
     * @param groupId
     * @param fileId
     * @param atime
     * @param ctime
     * @param mtime
     * @param size
     * @param perms
     * @param linkCount
     * @param epoch
     * @param issEpoch
     * @param readOnly
     */
    public BufferBackedFileMetadata(long parentId, String fileName, String ownerId, String groupId,
        long fileId, int atime, int ctime, int mtime, long size, int perms, long w32Atrrs, short linkCount,
        int epoch, int issEpoch, boolean readOnly) {
        
        // frequently changed metadata
        fcKeyBuf = generateKeyBuf(parentId, fileName, FC_METADATA);
        fcValBuf = ByteBuffer.wrap(new byte[20]).putInt(atime).putInt(ctime).putInt(mtime).putLong(size);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(parentId, fileName, ownerId, groupId, fileId, perms,
            w32Atrrs, linkCount, epoch, issEpoch, readOnly);
    }
    
    /**
     * Constructor for creating a new directory metadata object.
     * 
     * @param parentId
     * @param dirName
     * @param ownerId
     * @param groupId
     * @param fileId
     * @param atime
     * @param ctime
     * @param mtime
     * @param perms
     */
    public BufferBackedFileMetadata(long parentId, String dirName, String ownerId, String groupId,
        long fileId, int atime, int ctime, int mtime, int perms, long w32Attrs, short linkCount) {
        
        // frequently changed metadata
        fcKeyBuf = generateKeyBuf(parentId, dirName, FC_METADATA);
        fcValBuf = ByteBuffer.wrap(new byte[12]).putInt(atime).putInt(ctime).putInt(mtime);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(parentId, dirName, ownerId, groupId, fileId, perms, w32Attrs,
            linkCount);
    }
    
    @Override
    public long getId() {
        return rcMetadata.getId();
    }
    
    @Override
    public int getAtime() {
        return fcValBuf.getInt(FC_ATIME);
    }
    
    @Override
    public int getCtime() {
        return fcValBuf.getInt(FC_CTIME);
    }
    
    @Override
    public int getMtime() {
        return fcValBuf.getInt(FC_MTIME);
    }
    
    @Override
    public int getEpoch() {
        return rcMetadata.getEpoch();
    }
    
    @Override
    public int getIssuedEpoch() {
        return rcMetadata.getIssuedEpoch();
    }
    
    @Override
    public short getLinkCount() {
        return rcMetadata.getLinkCount();
    }
    
    @Override
    public int getPerms() {
        return rcMetadata.getPerms();
    }
    
    @Override
    public long getSize() {
        return fcValBuf.getLong(FC_SIZE);
    }
    
    @Override
    public boolean isReadOnly() {
        return rcMetadata.isReadOnly();
    }
    
    @Override
    public void setId(long id) {
        rcMetadata.setId(id);
    }
    
    @Override
    public void setAtime(int atime) {
        fcValBuf.putInt(FC_ATIME, atime);
    }
    
    @Override
    public void setCtime(int ctime) {
        fcValBuf.putInt(FC_CTIME, ctime);
    }
    
    @Override
    public void setMtime(int mtime) {
        fcValBuf.putInt(FC_MTIME, mtime);
    }
    
    @Override
    public void setEpoch(int epoch) {
        rcMetadata.setEpoch(epoch);
    }
    
    @Override
    public void setIssuedEpoch(int issuedEpoch) {
        rcMetadata.setIssuedEpoch(issuedEpoch);
    }
    
    @Override
    public void setLinkCount(short linkCount) {
        rcMetadata.setLinkCount(linkCount);
    }
    
    @Override
    public void setPerms(int perms) {
        rcMetadata.setPerms(perms);
    }
    
    @Override
    public void setReadOnly(boolean readOnly) {
        rcMetadata.setReadOnly(readOnly);
    }
    
    @Override
    public void setSize(long size) {
        fcValBuf.putLong(FC_SIZE, size);
    }
    
    @Override
    public void setW32Attrs(long w32Attrs) {
        rcMetadata.setW32Attrs(w32Attrs);
    }
    
    @Override
    public String getFileName() {
        return rcMetadata.getFileName();
    }
    
    @Override
    public String getOwnerId() {
        return rcMetadata.getOwnerId();
    }
    
    @Override
    public String getOwningGroupId() {
        return rcMetadata.getOwningGroupId();
    }
    
    @Override
    public long getW32Attrs() {
        return rcMetadata.getW32Attrs();
    }
    
    @Override
    public XLocList getXLocList() {
        return rcMetadata.getXLocList();
    }
    
    @Override
    public boolean isDirectory() {
        return rcMetadata.isDirectory();
    }
    
    @Override
    public void setXLocList(XLocList xlocList) {
        
        assert (!isDirectory()) : "cannot assign locations list to directory";
        assert (xlocList instanceof BufferBackedXLocList);
        
        BufferBackedXLocList bbXLocList = (BufferBackedXLocList) xlocList;
        rcMetadata.setXLocList(bbXLocList);
    }
    
    @Override
    public void setOwnerAndGroup(String owner, String group) {
        
        BufferBackedRCMetadata tmp = isDirectory() ? new BufferBackedRCMetadata(0, rcMetadata.getFileName(),
            owner, group, rcMetadata.getId(), rcMetadata.getPerms(), rcMetadata.getW32Attrs(), rcMetadata
                    .getLinkCount()) : new BufferBackedRCMetadata(0, rcMetadata.getFileName(), owner, group,
            rcMetadata.getId(), rcMetadata.getPerms(), rcMetadata.getW32Attrs(), rcMetadata.getLinkCount(),
            rcMetadata.getEpoch(), rcMetadata.getIssuedEpoch(), rcMetadata.isReadOnly());
        
        BufferBackedRCMetadata oldRCMetadata = rcMetadata;
        
        rcMetadata = new BufferBackedRCMetadata(rcMetadata == null ? null : rcMetadata.getKey(), tmp
                .getValue());
        
        if (!rcMetadata.isDirectory())
            rcMetadata.setXLocList(oldRCMetadata.getXLocList());
    }
    
    public byte[] getFCMetadataKey() {
        return fcKeyBuf == null ? null : fcKeyBuf.array();
    }
    
    public byte[] getFCMetadataValue() {
        return fcValBuf.array();
    }
    
    public BufferBackedRCMetadata getRCMetadata() {
        return rcMetadata;
    }
    
    public byte[] getKeyBuffer(int type) {
        
        switch (type) {
        case 0:
            return fcKeyBuf.array();
        case 1:
            return rcMetadata.getKey();
        }
        
        return null;
    }
    
    public byte[] getValueBuffer(byte type) {
        
        switch (type) {
        case 0:
            return fcValBuf.array();
        case 1:
            return rcMetadata.getValue();
        }
        
        return null;
    }
    
    public int getIndexId() {
        return indexId;
    }
    
    public String toString() {
        
        String s = (isDirectory() ? "dir" : "file") + " id=" + getId() + " name=" + getFileName() + " mode="
            + getPerms() + " w32Attrs=" + getW32Attrs() + " atime=" + getAtime() + " mtime=" + getMtime()
            + " ctime=" + getCtime() + " owner=" + getOwnerId() + " group=" + getOwningGroupId();
        if (!isDirectory())
            s += " size=" + getSize() + " epoch=" + getEpoch() + " issEpoch=" + getIssuedEpoch();
        
        return s;
    }
    
    protected static ByteBuffer generateKeyBuf(long parentId, String fileName, int type) {
        
        byte[] bytes = fileName.getBytes();
        
        byte[] tmp = new byte[9 + bytes.length];
        ByteBuffer buf = ByteBuffer.wrap(tmp);
        buf.putLong(parentId).put(bytes).put((byte) type);
        
        return buf;
    }
}
