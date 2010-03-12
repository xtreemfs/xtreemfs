package org.xtreemfs.osd.rwre;

import java.net.InetSocketAddress;
import java.util.List;

public class ObjectFetchRecord {

    private final long objNumber;

    private long objVersion;

    private final List<InetSocketAddress> osds;

    private int  osdToUse;

    public ObjectFetchRecord(long objNo, long objVer, List<InetSocketAddress> osds) {
        super();
        this.objNumber = objNo;
        this.objVersion = objVer;
        this.osds = osds;
        osdToUse = 0;
    }

    /**
     * @return the objNumber
     */
    public long getObjNumber() {
        return objNumber;
    }

    /**
     * @return the objVersion
     */
    public long getObjVersion() {
        return objVersion;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        try {
            ObjectFetchRecord ofr = (ObjectFetchRecord) o;
            return ofr.objNumber == this.objNumber;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    /**
     * @return the osds
     */
    public InetSocketAddress getNextOSD() {
        if (osdToUse < osds.size())
            return osds.get(osdToUse++);
        else
            return null;
    }

    List<InetSocketAddress> getOsds() {
        return osds;
    }

    /**
     * @param objVersion the objVersion to set
     */
    public void setObjVersion(long objVersion) {
        this.objVersion = objVersion;
    }

    public String toString() {
        return objNumber+"@"+objVersion+" osds: "+osds;
    }

}
