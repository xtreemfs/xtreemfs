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
package org.xtreemfs.new_mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedFileMetadata implements FileMetadata {
    
    public static final short            NUM_BUFFERS = 3;
    
    protected static final int           FC_ATIME    = 0;
    
    protected static final int           FC_CTIME    = 4;
    
    protected static final int           FC_MTIME    = 8;
    
    protected static final int           FC_SIZE     = 12;
    
    private final boolean                directory;
    
    private final ByteBuffer[]           keyBufs;
    
    private final ByteBuffer[]           valBufs;
    
    private final BufferBackedRCMetadata rcMetadata;
    
    private BufferBackedXLocList         xLocList;
    
    /**
     * Creates a new buffer-backed metadata object from a set of buffers.
     * 
     * @param keyBufs
     *            the database keys
     * @param valBufs
     *            the database values
     */
    public BufferBackedFileMetadata(byte[][] keyBufs, byte[][] valBufs) {
        
        assert (keyBufs.length == NUM_BUFFERS);
        assert (valBufs.length == NUM_BUFFERS);
        
        // assign the keys
        this.keyBufs = new ByteBuffer[NUM_BUFFERS];
        for (int i = 0; i < NUM_BUFFERS; i++)
            if (keyBufs[i] != null)
                this.keyBufs[i] = ByteBuffer.wrap(keyBufs[i]);
        
        // assign the values
        this.valBufs = new ByteBuffer[NUM_BUFFERS];
        for (int i = 0; i < NUM_BUFFERS; i++)
            if (valBufs[i] != null)
                this.valBufs[i] = ByteBuffer.wrap(valBufs[i]);
        
        rcMetadata = valBufs[RC_METADATA] == null ? null : new BufferBackedRCMetadata(
            keyBufs[RC_METADATA], valBufs[RC_METADATA]);
        xLocList = valBufs[XLOC_METADATA] == null ? null : new BufferBackedXLocList(
            valBufs[XLOC_METADATA]);
        directory = valBufs[FC_METADATA] == null ? null : valBufs[FC_METADATA].length == 12;
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
        long fileId, int atime, int ctime, int mtime, long size, short perms, short linkCount,
        int epoch, int issEpoch, boolean readOnly, short collCount) {
        
        //
        // assign the keys
        //
        
        keyBufs = new ByteBuffer[NUM_BUFFERS];
        keyBufs[FC_METADATA] = generateKeyBuf(parentId, fileName, FC_METADATA, collCount);
        keyBufs[RC_METADATA] = generateKeyBuf(parentId, fileName, RC_METADATA, collCount);
        
        //
        // assign the values
        //
        
        valBufs = new ByteBuffer[NUM_BUFFERS];
        
        // FC metadata
        valBufs[FC_METADATA] = ByteBuffer.wrap(new byte[20]);
        valBufs[FC_METADATA].putInt(atime).putInt(ctime).putInt(mtime).putLong(size);
        
        rcMetadata = new BufferBackedRCMetadata(parentId, fileName, ownerId, groupId, fileId,
            perms, linkCount, epoch, issEpoch, readOnly, collCount);
        xLocList = null;
        directory = false;
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
        long fileId, int atime, int ctime, int mtime, short perms, short collCount) {
        
        //
        // assign the keys
        //
        
        keyBufs = new ByteBuffer[NUM_BUFFERS];
        keyBufs[FC_METADATA] = generateKeyBuf(parentId, dirName, FC_METADATA, collCount);
        keyBufs[RC_METADATA] = generateKeyBuf(parentId, dirName, RC_METADATA, collCount);
        
        //
        // assign the values
        //
        
        valBufs = new ByteBuffer[NUM_BUFFERS];
        
        // FC metadata
        valBufs[FC_METADATA] = ByteBuffer.wrap(new byte[12]);
        valBufs[FC_METADATA].putInt(atime).putInt(ctime).putInt(mtime);
        
        rcMetadata = new BufferBackedRCMetadata(parentId, dirName, ownerId, groupId, fileId, perms,
            collCount);
        xLocList = null;
        directory = true;
    }
    
    @Override
    public long getId() {
        return rcMetadata.getId();
    }
    
    @Override
    public int getAtime() {
        return valBufs[FC_METADATA].getInt(FC_ATIME);
    }
    
    @Override
    public int getCtime() {
        return valBufs[FC_METADATA].getInt(FC_CTIME);
    }
    
    @Override
    public int getMtime() {
        return valBufs[FC_METADATA].getInt(FC_MTIME);
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
    public short getPerms() {
        return rcMetadata.getPerms();
    }
    
    @Override
    public long getSize() {
        return valBufs[FC_METADATA].getLong(FC_SIZE);
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
        valBufs[FC_METADATA].putInt(FC_ATIME, atime);
    }
    
    @Override
    public void setCtime(int ctime) {
        valBufs[FC_METADATA].putInt(FC_CTIME, ctime);
    }
    
    @Override
    public void setMtime(int mtime) {
        valBufs[FC_METADATA].putInt(FC_MTIME, mtime);
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
    public void setPerms(short perms) {
        rcMetadata.setPerms(perms);
    }
    
    @Override
    public void setReadOnly(boolean readOnly) {
        rcMetadata.setReadOnly(readOnly);
    }
    
    @Override
    public void setSize(long size) {
        valBufs[FC_METADATA].putLong(FC_SIZE, size);
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
    public XLocList getXLocList() {
        return xLocList;
    }
    
    @Override
    public boolean isDirectory() {
        return directory;
    }
    
    public void setXLocList(BufferBackedXLocList xloc) {
        assert (!directory) : "cannot assign locations list to directory";
        
        byte[] tmp = new byte[keyBufs[FC_SIZE].limit()];
        System.arraycopy(tmp, 0, keyBufs[XLOC_METADATA].array(), 0, tmp.length);
        keyBufs[XLOC_METADATA] = ByteBuffer.wrap(tmp).put(12, (byte) XLOC_METADATA);
        valBufs[XLOC_METADATA] = ByteBuffer.wrap(xloc.getBuffer());
        xLocList = new BufferBackedXLocList(xloc.getBuffer());
    }
    
    public byte[] getFCMetadataKey() {
        return keyBufs[FC_METADATA].array();
    }
    
    public byte[] getFCMetadataValue() {
        return valBufs[FC_METADATA].array();
    }
    
    public BufferBackedRCMetadata getRCMetadata() {
        return rcMetadata;
    }
    
    public byte[] getXLocListKey() {
        return keyBufs[XLOC_METADATA].array();
    }
    
    public byte[] getXLocListValue() {
        return valBufs[XLOC_METADATA].array();
    }
    
    public byte[] getKeyBuffer(int type) {
        return keyBufs[type].array();
    }
    
    public byte[] getValueBuffer(int type) {
        return valBufs[type].array();
    }
    
    private ByteBuffer generateKeyBuf(long parentId, String fileName, int type, short collCount) {
        
        byte[] tmp = new byte[collCount == 0 ? 13 : 15];
        ByteBuffer buf = ByteBuffer.wrap(tmp);
        buf.putLong(parentId).putInt(fileName.hashCode()).put((byte) type);
        if (collCount != 0)
            buf.putShort(collCount);
        
        return buf;
    }
}
