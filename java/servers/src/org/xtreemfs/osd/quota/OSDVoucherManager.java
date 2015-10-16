/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.quota;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.common.quota.QuotaConstants;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.storage.StorageLayout;

/**
 * This class handles all given vouchers on the OSD by managing a responsible manager per file.
 * 
 * All requests are splitted among the StorageThreads by fileId, so that concurrent access on a single file is not
 * possible. Therefore, this class doesn't have to be thread-safe.
 */
public class OSDVoucherManager {

    private final Map<String, FileVoucherManager> fileVoucherManagerMap = new HashMap<String, FileVoucherManager>();
    private final StorageLayout                   storageLayout;

    /**
     * 
     */
    public OSDVoucherManager(StorageLayout storageLayout) {
        this.storageLayout = storageLayout;
    }

    public void registerFileVoucher(String fileId, String clientId, long expireTime, long voucherSize)
            throws VoucherErrorException, IOException {

        if (voucherSize == QuotaConstants.UNLIMITED_VOUCHER) {
            return;
        }

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            fileVoucherManager = new FileVoucherManager(fileId, storageLayout);
            fileVoucherManagerMap.put(fileId, fileVoucherManager);
        }

        fileVoucherManager.addVoucher(clientId, expireTime, voucherSize);
    }

    public boolean checkMaxVoucherSize(String fileId, String clientId, long expireTime, long newFileSize)
            throws VoucherErrorException {

        boolean result = false;

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            // assume, that no voucher is registered
            return true;
        }

        result = fileVoucherManager.checkMaxVoucherSize(clientId, expireTime, newFileSize);

        return result;
    }

    public void invalidateFileVouchers(String fileId, String clientId, Set<Long> expireTimeSet) throws IOException {

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            fileVoucherManager = new FileVoucherManager(fileId, storageLayout);
            fileVoucherManagerMap.put(fileId, fileVoucherManager);
        }

        fileVoucherManager.invalidateVouchers(clientId, expireTimeSet);

        if (fileVoucherManager.isObsolete()) {
            fileVoucherManager.delete();
            fileVoucherManagerMap.remove(fileId);
        }
    }

    /**
     * 
     * @return uniform error response in case of an insufficient voucher error
     */
    public ErrorResponse getInsufficientVoucherErrorResponse() {
        return ErrorUtils.getErrorResponse(ErrorType.INSUFFICIENT_VOUCHER, POSIXErrno.POSIX_ERROR_NONE,
                "The new filesize is larger than the maximum allocated one via vouchers");
    }
}
