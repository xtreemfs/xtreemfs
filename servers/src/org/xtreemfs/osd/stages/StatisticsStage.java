/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.stages;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xtreemfs.common.RingBuffer;
import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.common.logging.Logging;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.pinky.HTTPUtils.DATA_TYPE;
import org.xtreemfs.osd.ErrorRecord;
import org.xtreemfs.osd.OSDRequestDispatcher;
import org.xtreemfs.osd.RequestDispatcher.Stages;

/**
 *
 * @author bjko
 */
public final class StatisticsStage extends Stage {
    
    public static final int STAGEOP_STATUS_PAGE = 1;
    
    public static final int STAGEOP_STATISTICS = 2;
    
    public static final int STAGEOP_MEASURE_RQT = 3;
    
    public static boolean measure_request_times;
    
    public static boolean collect_statistics;
    
    private final RingBuffer<Long>    txRate;
    private final RingBuffer<Long>    rxRate;
    private final RingBuffer<Long>    cpu;
    private final RingBuffer<Long>    readRate;
    private final RingBuffer<Long>    writeRate;
    private final RingBuffer<Long>    memUsage;
    private final RingBuffer<Long>[]    stagesAvg;
    private final RingBuffer<Long>[]    stagesMin;
    private final RingBuffer<Long>[]    stagesMax; 
    
    private final StageStatistics stats;
    
    private StatisticsCollector collector;
    private final int statlength;
    
    private final Stages[]            statStages;
    
    private final OSDRequestDispatcher master;
    
    public StatisticsStage(OSDRequestDispatcher master, StageStatistics stats, int statlength) {
        super("StatStage");
        this.master = master;
        this.statlength = statlength;
        txRate = new RingBuffer(statlength,0l);
        rxRate = new RingBuffer(statlength,0l);
        cpu = new RingBuffer(statlength,0l);
        readRate = new RingBuffer(statlength,0l);
        writeRate = new RingBuffer(statlength,0l);
        memUsage = new RingBuffer(statlength,0l);
        
        statStages = new Stages[]{Stages.PARSER, Stages.AUTH, Stages.STORAGE};
        
        final int numStages = statStages.length;
        stagesAvg = new RingBuffer[numStages];
        stagesMin = new RingBuffer[numStages];
        stagesMax = new RingBuffer[numStages];
        for (int i = 0; i < stagesAvg.length; i++) {
            stagesAvg[i] = new RingBuffer(statlength,0l);
            stagesMin[i] = new RingBuffer(statlength,0l);
            stagesMax[i] = new RingBuffer(statlength,0l);
        }
        this.stats = stats;
        
        StatisticsStage.measure_request_times = master.getConfig().isMeasureRqsEnabled();
        StatisticsStage.collect_statistics = master.getConfig().isBasicStatsEnabled();
        if (StatisticsStage.collect_statistics) {
            collector = new StatisticsCollector(stats);
        }
    }
    
    public void start() {
        super.start();
        if (collector != null)
            collector.start();
    }
    
    public void shutdown() {
        if (collector != null) {
            collector.quit = true;
            collector.interrupt();
        }
        super.shutdown();
    }
    
    
    @Override
    protected void processMethod(StageMethod method) {
        if (method.getStageMethod() == STAGEOP_STATISTICS) {
            processStageOpStatistics(method); 
        } else if (method.getStageMethod() == STAGEOP_MEASURE_RQT) {
            processStageOpSettings(method);
            
        }
    }

    private void processStageOpSettings(StageMethod method) {
        final Boolean[] settings = (Boolean[]) method.getRq().getAttachment();
        final Boolean enableRqMeasurements = settings[0];
        final Boolean enableBasicStats = settings[1];
        if (enableRqMeasurements != null) {
            StatisticsStage.measure_request_times = enableRqMeasurements.booleanValue();
        }
        if (enableBasicStats != null) {
            StatisticsStage.collect_statistics = enableBasicStats.booleanValue();
            if (enableBasicStats) {
                if (collector == null) {
                    collector = new StatisticsCollector(stats);
                    collector.start();
                }
            } else {
                if (collector != null) {
                    collector.quit = true;
                    collector.interrupt();
                    collector = null;
                }
            }
        }
        try {
            List<Object> data = new ArrayList(2);
            data.add(new Boolean(StatisticsStage.measure_request_times));
            data.add(new Boolean(StatisticsStage.collect_statistics));
            method.getRq().setData(ReusableBuffer.wrap(JSONParser.writeJSON(data).getBytes()), DATA_TYPE.JSON);
            methodExecutionSuccess(method, StageResponseCode.FINISH);
        } catch (JSONException ex) {
            methodExecutionFailed(method, new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, ex.getMessage()));
        }
    }

    private void processStageOpStatistics(StageMethod method) {
        Map<String, Object> data = new HashMap();
        
        if (StatisticsStage.collect_statistics) {
            List<Long> txSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = txRate.iterator();
                while (iter.hasNext()) {
                    txSeries.add(iter.next());
                }
            }
            data.put("TX", txSeries);

            List<Long> rxSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = rxRate.iterator();
                while (iter.hasNext()) {
                    rxSeries.add(iter.next());
                }
            }
            data.put("RX", rxSeries);

            List<Long> cpuSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = cpu.iterator();
                while (iter.hasNext()) {
                    cpuSeries.add(iter.next());
                }
            }
            data.put("CPU", cpuSeries);

            List<Long> readSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = readRate.iterator();
                while (iter.hasNext()) {
                    readSeries.add(iter.next());
                }
            }
            data.put("READ", readSeries);

            List<Long> writeSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = writeRate.iterator();
                while (iter.hasNext()) {
                    writeSeries.add(iter.next());
                }
            }
            data.put("WRITE", writeSeries);

            List<Long> memSeries = new LinkedList();
            synchronized (txRate) {
                Iterator<Long> iter = memUsage.iterator();
                while (iter.hasNext()) {
                    memSeries.add(iter.next());
                }
            }
            data.put("MEM", memSeries);

            data.put("FREEDISK", ((OSDRequestDispatcher) master).getFreeSpace());

            data.put("TOTALDISK", ((OSDRequestDispatcher) master).getTotalSpace());

            data.put("USEDMEM", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());

            data.put("TOTALMEM", Runtime.getRuntime().maxMemory());
        }
        
        if (StatisticsStage.measure_request_times) {
            
            for (int st = 0; st < statStages.length; st++) {
                final String stName = statStages[st].toString();
                List<Long> avgs = new LinkedList();
                synchronized (txRate) {
                    Iterator<Long> iter = stagesAvg[st].iterator();
                    while (iter.hasNext()) {
                        avgs.add(iter.next());
                    }
                }
                data.put(stName+"_AVG", avgs);

                List<Long> min = new LinkedList();
                synchronized (txRate) {
                    Iterator<Long> iter = stagesMin[st].iterator();
                    while (iter.hasNext()) {
                        min.add(iter.next());
                    }
                }
                data.put(stName+"_MIN", min);

                List<Long> max = new LinkedList();
                synchronized (txRate) {
                    Iterator<Long> iter = stagesMax[st].iterator();
                    while (iter.hasNext()) {
                        max.add(iter.next());
                    }
                }
                data.put(stName+"_MAX", max);
            }
        }

        

        try {
            method.getRq().setData(ReusableBuffer.wrap(JSONParser.writeJSON(data).getBytes()), DATA_TYPE.JSON);
            methodExecutionSuccess(method, StageResponseCode.FINISH);
        } catch (JSONException ex) {
            methodExecutionFailed(method, new ErrorRecord(ErrorRecord.ErrorClass.INTERNAL_SERVER_ERROR, ex.getMessage()));
        }
    }
    
    private final class StatisticsCollector extends Thread {
        
        public static final long INTEVRAL = 1000;
        
        private transient boolean quit;
        
        private final StageStatistics stats;
        
        public StatisticsCollector(StageStatistics stats) {
            quit = false;
            this.stats = stats;
        }
        
        @Override
        public void run() {
            long lastTx = stats.bytesTX.get();
            long lastRx = stats.bytesRX.get();
            long lastRead = stats.numReads.get();
            long lastWrite = stats.numWrites.get();
            OperatingSystemMXBean osb = ManagementFactory.getOperatingSystemMXBean();
            Logging.logMessage(Logging.LEVEL_INFO, this,"statistics collector started");
            do {
                try {
                    sleep(INTEVRAL);
                    
                    long newTx = stats.bytesTX.get();
                    long tx = newTx - lastTx;
                    lastTx = newTx;

                    long newRx = stats.bytesRX.get();
                    long rx = newRx - lastRx;
                    lastRx = newRx;
                    
                    
                    long newRead = stats.numReads.get();
                    long read = newRead - lastRead;
                    lastRead = newRead;
                    
                    long newWrite = stats.numWrites.get();
                    long write = newWrite - lastWrite;
                    lastWrite = newWrite;

                    synchronized (txRate) {
                        txRate.insert(tx);
                        rxRate.insert(rx);
                        cpu.insert(Long.valueOf((long)osb.getSystemLoadAverage()*1000));
                        readRate.insert(read);
                        writeRate.insert(write);
                        memUsage.insert(Runtime.getRuntime().freeMemory()*100/Runtime.getRuntime().maxMemory());
                        if (StatisticsStage.measure_request_times) {
                            //parserStage results
                            int i = 0;
                            for (Stages st : statStages) {
                                final Stage pStage = master.getStage(st);
                                long numRq = pStage._numRq.getAndSet(0);
                                long sumRq = pStage._sumRqTime.getAndSet(0);
                                long avgTime = (numRq > 0) ? sumRq/numRq : 0;
                                stagesAvg[i].insert(avgTime);
                                long maxTime = pStage._maxRqTime.getAndSet(0);
                                long minTime = pStage._minRqTime.getAndSet(Integer.MAX_VALUE);
                                stagesMax[i].insert(maxTime);
                                if (minTime < Integer.MAX_VALUE)
                                    stagesMin[i].insert(minTime);
                                else
                                    stagesMin[i].insert(Long.valueOf(0));
                                i++;
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    break;
                }
                
            } while (!quit);
            Logging.logMessage(Logging.LEVEL_INFO, this,"statistics collector stopped");
        }
        
    }

}
