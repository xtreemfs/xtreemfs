/*
 * Copyright (c) 2016 by Robert Bärhold, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.mrc.quota;

/**
 * Contains all the information about the issued voucher or the information, that no voucher could be issued. If no
 * voucher could be issued, the responsible quota being enforced will be provided as proper information.
 */
public class Voucher {

    private String      enforcedQuotaName = "";
    private long        voucherSize;
    private VoucherType voucherType;

    public Voucher(VoucherType voucherType, long voucherSize) {
        setVoucherType(voucherType);
        setVoucherSize(voucherSize);
    }

    // Getter

    public String getEnforcedQuotaName() {
        return enforcedQuotaName;
    }

    public long getVoucherSize() {
        return voucherSize;
    }

    public VoucherType getVoucherType() {
        return voucherType;
    }

    // Setter

    public void setEnforcedQuotaName(String enforcedQuotaName) {
        this.enforcedQuotaName = enforcedQuotaName;
    }

    public void setVoucherSize(long voucherSize) {
        if (voucherType != VoucherType.LIMITED && voucherSize != 0) {
            throw new IllegalArgumentException("The voucher size has to be 0, if it is not limited.");
        }
        this.voucherSize = voucherSize;
    }

    public void setVoucherType(VoucherType voucherType) {
        this.voucherType = voucherType;

        if (voucherType != VoucherType.LIMITED) {
            voucherSize = 0;
        }
    }

    public enum VoucherType {
        NONE, LIMITED, UNLIMITED;
    }
}
