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

import java.io.IOException;

/**
 * 
 * <br>
 * 17.08.2009
 */
public class MonitoringLog implements MonitoringListener<Double> {
    private static MonitoringLog instance;

    private long                 monitoringStartTime;

    public static synchronized void initialize(String filepath) throws IOException {
        if (instance == null) {
            instance = new MonitoringLog();

            instance.monitoringStartTime = System.currentTimeMillis();

            // file to write to
            // (new File(filepath)).getParentFile().mkdirs();
            // instance.out = new FileWriter(filepath);
        }
    }

    @Override
    public void valueAddedOrChanged(MonitoringEvent<Double> event) {
        monitor(event.getKey(), event.getNewValue().toString());
    }

    public static synchronized void monitor(String key, String value) {
        long time = (System.currentTimeMillis() - instance.monitoringStartTime) / 1000;
        System.out.println("[" + time + "s]\t" + key + "\t:\t" + value);
    }

    @SuppressWarnings("unchecked")
    public static void registerFor(Monitoring monitoring, String... keys) {
        for (String key : keys)
            monitoring.registerListener(key, instance);
    }
}
