/*
 * Copyright (c) 2015 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.osd.quota;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.ErrorType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.POSIXErrno;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.RPCHeader.ErrorResponse;
import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils;
import org.xtreemfs.osd.OSDRequestDispatcher;

/**
 * This class handles all given vouchers on the OSD by managing a responsible manager per file.
 * 
 * All requests are splitted among the StorageThreads by fileId, so that concurrent access on a single file not possible
 * is. Therefore, this class doesn't have to be thread-safe.
 */
public class OSDVoucherManager {

    private final static boolean                  active                = true;

    @SuppressWarnings("unused")
    // FIXME: remove unused?
    private final OSDRequestDispatcher            master;
    private final Map<String, FileVoucherManager> fileVoucherManagerMap = new HashMap<String, FileVoucherManager>();

    /**
     * 
     */
    public OSDVoucherManager(OSDRequestDispatcher dispatcher) {
        master = dispatcher;
    }

    public void registerFileVoucher(String fileId, String clientId, long expireTime, long voucherSize) {

        if (voucherSize <= 0 || !active) {
            return;
        }

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            fileVoucherManager = new FileVoucherManager(fileId);
            fileVoucherManagerMap.put(fileId, fileVoucherManager);
        }

        fileVoucherManager.addVoucher(clientId, expireTime, voucherSize);
    }

    public boolean checkMaxVoucherSize(String fileId, String clientId, long expireTime, long newFileSize)
            throws VoucherErrorException {

        if (!active) {
            return true;
        }

        boolean result = false;

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            // assume, that no voucher is registered
            return true;
        }

        result = fileVoucherManager.checkMaxVoucherSize(clientId, expireTime, newFileSize);

        return result;
    }

    public void invalidateFileVouchers(String fileId, String clientId, Set<Long> expireTimeSet) {

        if (!active) {
            return;
        }

        FileVoucherManager fileVoucherManager = fileVoucherManagerMap.get(fileId);
        if (fileVoucherManager == null) {
            fileVoucherManager = new FileVoucherManager(fileId);
            fileVoucherManagerMap.put(fileId, fileVoucherManager);
        }

        fileVoucherManager.invalidateVouchers(clientId, expireTimeSet);

        if (fileVoucherManager.isObsolete()) {
            System.out.println("Remove Obsolete Voucher"); // FIXME(remove)
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
