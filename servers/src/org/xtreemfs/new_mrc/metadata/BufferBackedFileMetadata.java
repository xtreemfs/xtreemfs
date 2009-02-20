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
    
    public static final short      NUM_BUFFERS = 3;
    
    protected static final int     FC_ATIME    = 0;
    
    protected static final int     FC_CTIME    = 4;
    
    protected static final int     FC_MTIME    = 8;
    
    protected static final int     FC_SIZE     = 12;
    
    private final ByteBuffer       fcKeyBuf;
    
    private final ByteBuffer       fcValBuf;
    
    private ByteBuffer             xLocKeyBuf;
    
    private BufferBackedRCMetadata rcMetadata;
    
    private BufferBackedXLocList   xLocList;
    
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
        
        assert (keyBufs == null || keyBufs.length == NUM_BUFFERS);
        assert (valBufs.length == NUM_BUFFERS);
        
        // frequently changed metadata
        fcKeyBuf = keyBufs == null ? null : ByteBuffer.wrap(keyBufs[FC_METADATA]);
        fcValBuf = ByteBuffer.wrap(valBufs[FC_METADATA]);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(keyBufs == null ? null : keyBufs[RC_METADATA],
            valBufs[RC_METADATA]);
        
        // XLocList metadata
        if (valBufs[XLOC_METADATA] != null) {
            xLocList = new BufferBackedXLocList(valBufs[XLOC_METADATA]);
            if (keyBufs != null)
                xLocKeyBuf = ByteBuffer.wrap(keyBufs[XLOC_METADATA]);
        }
        
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
        long fileId, int atime, int ctime, int mtime, long size, int perms, long w32Atrrs,
        short linkCount, int epoch, int issEpoch, boolean readOnly, short collCount) {
        
        // frequently changed metadata
        fcKeyBuf = generateKeyBuf(parentId, fileName, FC_METADATA, collCount);
        fcValBuf = ByteBuffer.wrap(new byte[20]).putInt(atime).putInt(ctime).putInt(mtime).putLong(
            size);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(parentId, fileName, ownerId, groupId, fileId,
            perms, w32Atrrs, linkCount, epoch, issEpoch, readOnly, collCount);
        
        // xLocList metadata
        xLocList = null;
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
        long fileId, int atime, int ctime, int mtime, int perms, long w32Attrs, short linkCount,
        short collCount) {
        
        // frequently changed metadata
        fcKeyBuf = generateKeyBuf(parentId, dirName, FC_METADATA, collCount);
        fcValBuf = ByteBuffer.wrap(new byte[12]).putInt(atime).putInt(ctime).putInt(mtime);
        
        // rarely changed metadata
        rcMetadata = new BufferBackedRCMetadata(parentId, dirName, ownerId, groupId, fileId, perms,
            w32Attrs, linkCount, collCount);
        
        // xLocList metadata
        xLocList = null;
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
        return xLocList;
    }
    
    @Override
    public boolean isDirectory() {
        return rcMetadata.isDirectory();
    }
    
    @Override
    public void setXLocList(XLocList xlocList) {
        
        assert (!isDirectory()) : "cannot assign locations list to directory";
        assert (xlocList instanceof BufferBackedXLocList);
        
        BufferBackedXLocList xloc = (BufferBackedXLocList) xlocList;
        
        if (fcKeyBuf != null) {
            byte[] tmp = new byte[fcKeyBuf.limit()];
            System.arraycopy(fcKeyBuf.array(), 0, tmp, 0, tmp.length);
            xLocKeyBuf = ByteBuffer.wrap(tmp).put(12, (byte) XLOC_METADATA);
        }
        xLocList = new BufferBackedXLocList(xloc.getBuffer());
    }
    
    @Override
    public void setOwnerAndGroup(String owner, String group) {
        
        BufferBackedRCMetadata tmp = isDirectory() ? new BufferBackedRCMetadata(0, rcMetadata
                .getFileName(), owner, group, rcMetadata.getId(), rcMetadata.getPerms(), rcMetadata
                .getW32Attrs(), rcMetadata.getLinkCount(), (short) 0) : new BufferBackedRCMetadata(
            0, rcMetadata.getFileName(), owner, group, rcMetadata.getId(), rcMetadata.getPerms(),
            rcMetadata.getW32Attrs(), rcMetadata.getLinkCount(), rcMetadata.getEpoch(), rcMetadata
                    .getIssuedEpoch(), rcMetadata.isReadOnly(), (short) 0);
        
        rcMetadata = new BufferBackedRCMetadata(rcMetadata == null ? null : rcMetadata.getKey(),
            tmp.getValue());
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
    
    public byte[] getXLocListKey() {
        return xLocKeyBuf == null ? null : xLocKeyBuf.array();
    }
    
    public byte[] getXLocListValue() {
        return xLocList == null ? null : xLocList.getBuffer();
    }
    
    public byte[] getKeyBuffer(int type) {
        
        switch (type) {
        case 0:
            return fcKeyBuf.array();
        case 1:
            return rcMetadata.getKey();
        case 2:
            return xLocKeyBuf.array();
        }
        
        return null;
    }
    
    public byte[] getValueBuffer(byte type) {
        
        switch (type) {
        case 0:
            return fcValBuf.array();
        case 1:
            return rcMetadata.getValue();
        case 2:
            return xLocList.getBuffer();
        }
        
        return null;
    }
    
    public int getIndexId() {
        return indexId;
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
