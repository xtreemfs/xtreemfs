/*
 * Copyright (c) 2009-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
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
