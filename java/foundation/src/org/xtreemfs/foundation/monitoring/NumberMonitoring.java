/*
 * Copyright (c) 2009-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.monitoring;

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
     * Increases the old value about new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Double putIncreaseFor(String key, Double value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            return super.put(key, oldValue + value);
        } else
            return super.put(key, value);
    }

    /**
     * Decreases the old value about new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Double putDecreaseFor(String key, Double value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            return super.put(key, oldValue - value);
        } else
            return super.put(key, value);
    }

    /**
     * Special method for Longs.
     * 
     * @see org.xtreemfs.foundation.monitoring.Monitoring#put(java.lang.String, java.lang.Object)
     * @param key
     * @param value
     * @return
     */
    public Long putLong(String key, Long value) {
        Double oldValue = super.put(key, value.doubleValue());
        return (oldValue == null) ? null : oldValue.longValue();
    }

    /**
     * Special method for Longs. Saves the value only if the new value is smaller than the old one.
     * 
     * @see org.xtreemfs.foundation.monitoring.NumberMonitoring#putSmaller(java.lang.String, java.lang.Double)
     * @param key
     * @param value
     * @return
     */
    public Long putSmallerLong(String key, Long value) {
        Double oldValue = this.putSmaller(key, value.doubleValue());
        return (oldValue == null) ? null : oldValue.longValue();
    }

    /**
     * Special method for Longs. Saves the value only if the new value is larger than the old one.
     * 
     * @see org.xtreemfs.foundation.monitoring.NumberMonitoring#putLarger(java.lang.String, java.lang.Double)
     * @param key
     * @param value
     * @return
     */
    public Long putLargerLong(String key, Long value) {
        Double oldValue = this.putLarger(key, value.doubleValue());
        return (oldValue == null) ? null : oldValue.longValue(); 
    }

    /**
     * Special method for Longs. Saves the average of the old and new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Long putAverageLong(String key, Long value) {
        Double oldValue = super.get(key);
        if (oldValue != null) {
            this.put(key, (oldValue + value) / 2d).longValue();
            return oldValue.longValue();
        } else
            return this.putLong(key, value);
    }

    /**
     * Increases the old value about new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Long putIncreaseForLong(String key, Long value) {
        Double oldValue = this.putIncreaseFor(key, value.doubleValue());
        return (oldValue == null) ? null : oldValue.longValue(); 
    }

    /**
     * Decreases the old value about new value.
     * 
     * @param key
     * @param value
     * @return
     */
    public Long putDecreaseForLong(String key, Long value) {
        Double oldValue = this.putDecreaseFor(key, value.doubleValue());
        return (oldValue == null) ? null : oldValue.longValue(); 
    }

    /**
     * 
     * @param key
     * @return
     */
    public Long getLong(String key) {
        Double value = super.get(key);
        return (value == null) ? null : value.longValue(); 
    }
}
