/*  Copyright (c) 2009 Barcelona Supercomputing Center - Centro Nacional
    de Supercomputacion and Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.monitoring;

/**
 * The class provides the ability to monitor numeric data. It also provides methods some methods for special
 * cases than only overwriting the old value.<br>
 * NOTE: This class is thread-safe. <br>
 * 22.07.2009
 */
public class NumberMonitoring extends Monitoring<Double> {
    /**
     * Saves the value only if the new value is smaller than the old one.
     * 
     * @param key
     * @param value
     * @return
     */
    public Double putSmaller(String key, Double value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            if (oldValue > value)
                return super.put(key, value);
            else
                return value;
        } else
            return super.put(key, value);
    }

    /**
     * Saves the value only if the new value is larger than the old one.
     * 
     * @param key
     * @param value
     * @return
     */
    public Double putLarger(String key, Double value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            if (oldValue < value)
                return super.put(key, value);
            else
                return value;
        } else
            return super.put(key, value);
    }

    /**
     * Saves the average of the old and new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Double putAverage(String key, Double value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            return super.put(key, (oldValue + value) / 2);
        } else
            return super.put(key, value);
    }

    /**
     * Special method for Longs.
     * 
     * @see org.xtreemfs.common.monitoring.Monitoring#put(java.lang.String, java.lang.Object)
     * @param key
     * @param value
     * @return
     */
    public long putLong(String key, Long value) {
        return super.put(key, value.doubleValue()).longValue();
    }

    /**
     * Special method for Longs. Saves the value only if the new value is smaller than the old one.
     * 
     * @see org.xtreemfs.common.monitoring.NumberMonitoring#putSmaller(java.lang.String, java.lang.Double)
     * @param key
     * @param value
     * @return
     */
    public long putSmallerLong(String key, Long value) {
        return this.putSmaller(key, value.doubleValue()).longValue();
    }

    /**
     * Special method for Longs. Saves the value only if the new value is larger than the old one.
     * 
     * @see org.xtreemfs.common.monitoring.NumberMonitoring#putLarger(java.lang.String, java.lang.Double)
     * @param key
     * @param value
     * @return
     */
    public long putLargerLong(String key, Long value) {
        return this.putLarger(key, value.doubleValue()).longValue();
    }

    /**
     * Special method for Longs. Saves the average of the old and new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public long putAverageLong(String key, Long value) {
        Long oldValue = super.get(key).longValue();
        if (oldValue != null) {
            return this.putLong(key, (oldValue + value) / 2);
        } else
            return this.putLong(key, value);
    }

    /**
     * 
     * @param key
     * @return
     */
    public long getLong(String key) {
        return super.get(key).longValue();
    }
}
