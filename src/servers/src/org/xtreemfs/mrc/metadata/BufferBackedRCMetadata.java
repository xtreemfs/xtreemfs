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

package org.xtreemfs.mrc.metadata;

import java.nio.ByteBuffer;

public class BufferBackedRCMetadata {
    
    protected static final int   RC_TYPE                  = 0;
    
    protected static final int   RC_ID                    = 1;
    
    protected static final int   RC_PERMS                 = 9;
    
    protected static final int   RC_LINKCOUNT             = 13;
    
    protected static final int   RC_W32ATTRS              = 15;
    
    protected static final int   RC_EPOCH                 = 23;
    
    protected static final int   RC_ISSEPOCH              = 27;
    
    protected static final int   RC_READONLY              = 31;
    
    protected static final int   RC_DIR_GROUP_OFFSET      = 23;
    
    protected static final int   DIR_VAR_LEN_PART_OFFSET  = 25;
    
    protected static final int   RC_FILE_GROUP_OFFSET     = 32;
    
    protected static final int   RC_XLOC_OFFSET           = 34;
    
    protected static final int   FILE_VAR_LEN_PART_OFFSET = 36;
    
    private final short          groupOffset;
    
    private final short          xLocOffset;
    
    private final boolean        directory;
    
    private final ByteBuffer     keyBuf;
    
    private ByteBuffer           valBuf;
    
    private BufferBackedXLocList cachedXLocList;
    
    /**
     * Creates a new buffer-backed metadata object from a key and a value
     * buffer.
     * 
     * @param keyBuf
     *            the database key
     * @param valBuf
     *            the database value
     */
    public BufferBackedRCMetadata(byte[] keyBuf, byte[] valBuf) {
        
        // assign the key and value
        this.keyBuf = keyBuf == null ? null : ByteBuffer.wrap(keyBuf);
        this.valBuf = ByteBuffer.wrap(valBuf);
        
        directory = valBuf[0] == 1;
        groupOffset = this.valBuf.getShort(directory ? RC_DIR_GROUP_OFFSET : RC_FILE_GROUP_OFFSET);
        xLocOffset = directory ? (short) valBuf.length : this.valBuf.getShort(RC_XLOC_OFFSET);
    }
    
    /**
     * Creates a new buffer-backed file metadata object.
     * 
     * @param parentId
     * @param fileName
     * @param ownerId
     * @param groupId
     * @param fileId
     * @param perms
     * @param linkCount
     * @param epoch
     * @param issEpoch
     * @param readOnly
     */
    public BufferBackedRCMetadata(long parentId, String fileName, String ownerId, String groupId,
        long fileId, int perms, long w32Attrs, short linkCount, int epoch, int issEpoch, boolean readOnly) {
        
        // assign the key
        keyBuf = BufferBackedFileMetadata.generateKeyBuf(parentId, fileName,
            BufferBackedFileMetadata.RC_METADATA);
        
        // assign the value
        byte[] oBytes = ownerId.getBytes();
        byte[] gBytes = groupId.getBytes();
        
        final int bufSize = FILE_VAR_LEN_PART_OFFSET + oBytes.length + gBytes.length;
        groupOffset = (short) (bufSize - gBytes.length);
        xLocOffset = (short) bufSize;
        
        valBuf = ByteBuffer.wrap(new byte[bufSize]);
        valBuf.put((byte) 0).putLong(fileId).putInt(perms).putShort(linkCount).putLong(w32Attrs)
                .putInt(epoch).putInt(issEpoch).put((byte) (readOnly ? 1 : 0)).putShort(groupOffset)
                .putShort(xLocOffset).put(oBytes).put(gBytes);
        
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
     * @param perms
     */
    public BufferBackedRCMetadata(long parentId, String dirName, String ownerId, String groupId, long fileId,
        int perms, long w32Attrs, short linkCount) {
        
        // assign the key
        keyBuf = BufferBackedFileMetadata.generateKeyBuf(parentId, dirName,
            BufferBackedFileMetadata.RC_METADATA);
        
        // assign the value
        byte[] oBytes = ownerId.getBytes();
        byte[] gBytes = groupId.getBytes();
        
        final int bufSize = DIR_VAR_LEN_PART_OFFSET + oBytes.length + gBytes.length;
        groupOffset = (short) (bufSize - gBytes.length);
        xLocOffset = (short) bufSize;
        
        valBuf = ByteBuffer.wrap(new byte[bufSize]);
        valBuf.put((byte) 1).putLong(fileId).putInt(perms).putShort(linkCount).putLong(w32Attrs).putShort(
            groupOffset).put(oBytes).put(gBytes);
        
        directory = true;
    }
    
    public long getId() {
        return valBuf.getLong(RC_ID);
    }
    
    public byte getType() {
        return valBuf.get();
    }
    
    public int getEpoch() {
        return valBuf.getInt(RC_EPOCH);
    }
    
    public int getIssuedEpoch() {
        return valBuf.getInt(RC_ISSEPOCH);
    }
    
    public short getLinkCount() {
        return valBuf.getShort(RC_LINKCOUNT);
    }
    
    public int getPerms() {
        return valBuf.getInt(RC_PERMS);
    }
    
    public BufferBackedXLocList getXLocList() {
        
        if (cachedXLocList == null) {
            
            byte[] bytes = valBuf.array();
            int index = valBuf.getShort(RC_XLOC_OFFSET);
            
            if (bytes.length - index > 0)
                cachedXLocList = new BufferBackedXLocList(bytes, index, bytes.length - index);
        }
        
        return cachedXLocList;
    }
    
    public boolean isReadOnly() {
        return valBuf.get(RC_READONLY) != 0;
    }
    
    public void setId(long id) {
        valBuf.putLong(RC_ID, id);
    }
    
    public void setEpoch(int epoch) {
        valBuf.putInt(RC_EPOCH, epoch);
    }
    
    public void setIssuedEpoch(int issuedEpoch) {
        valBuf.putInt(RC_ISSEPOCH, issuedEpoch);
    }
    
    public void setLinkCount(short linkCount) {
        valBuf.putShort(RC_LINKCOUNT, linkCount);
    }
    
    public void setPerms(int perms) {
        valBuf.putInt(RC_PERMS, perms);
    }
    
    public void setReadOnly(boolean readOnly) {
        valBuf.put(RC_READONLY, (byte) (readOnly ? 1 : 0));
    }
    
    public void setW32Attrs(long w32Attrs) {
        valBuf.putLong(RC_W32ATTRS, w32Attrs);
    }
    
    public void setXLocList(BufferBackedXLocList xLocList) {
        
        int index = valBuf.getShort(RC_XLOC_OFFSET);
        
        byte[] bytes = this.valBuf.array();
        byte[] xLocBytes = xLocList == null? new byte[0]: xLocList.getBuffer();
        byte[] tmp = new byte[index + xLocBytes.length];
        
        System.arraycopy(bytes, 0, tmp, 0, index);
        System.arraycopy(xLocBytes, 0, tmp, index, xLocBytes.length);
        this.valBuf = ByteBuffer.wrap(tmp);
        
        cachedXLocList = xLocList;
    }
    
    public String getFileName() {
        byte[] bytes = keyBuf.array();
        return new String(bytes, 8, bytes.length - 9);
    }
    
    public String getOwnerId() {
        int index = directory ? DIR_VAR_LEN_PART_OFFSET : FILE_VAR_LEN_PART_OFFSET;
        int length = groupOffset - index;
        return new String(valBuf.array(), index, length);
    }
    
    public String getOwningGroupId() {
        int startIndex = groupOffset;
        int endIndex = directory ? valBuf.limit() : xLocOffset;
        int length = endIndex - startIndex;
        return new String(valBuf.array(), startIndex, length);
    }
    
    public long getW32Attrs() {
        return valBuf.getLong(RC_W32ATTRS);
    }
    
    public boolean isDirectory() {
        return directory;
    }
    
    public byte[] getKey() {
        return keyBuf == null ? null : keyBuf.array();
    }
    
    public byte[] getValue() {
        return valBuf.array();
    }
    
}
