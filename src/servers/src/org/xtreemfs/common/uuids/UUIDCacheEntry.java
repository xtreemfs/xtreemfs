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
 * AUTHORS: Björn Kolbeck (ZIB)
 */
package org.xtreemfs.common.uuids;

import java.net.InetSocketAddress;
import org.xtreemfs.common.TimeSync;

/**
 * Cache entry for the UUIDResolver.
 * @author bjko
 */
class UUIDCacheEntry {

    private String uuid;

    private InetSocketAddress resolvedAddr;

    private long validUntil;

    private long lastAccess;

    private String protocol;

    private boolean sticky;

    public UUIDCacheEntry(String uuid, String protocol, InetSocketAddress resolvedAddr,
            long validUntil) {
        this.uuid = uuid;
        this.protocol = protocol;
        this.resolvedAddr = resolvedAddr;
        this.validUntil = validUntil;
        this.lastAccess = TimeSync.getLocalSystemTime();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public InetSocketAddress getResolvedAddr() {
        return resolvedAddr;
    }

    public void setResolvedAddr(InetSocketAddress resolvedAddr) {
        this.resolvedAddr = resolvedAddr;
    }

    public long getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(long validUntil) {
        this.validUntil = validUntil;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public void setLastAccess(long lastAccess) {
        this.lastAccess = lastAccess;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }
}
