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
package org.xtreemfs.sandbox.tests.replicationStressTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;

import org.xtreemfs.osd.OSD;
import org.xtreemfs.osd.OSDConfig;
import org.xtreemfs.utils.CLIParser;
import org.xtreemfs.utils.xtfs_repl;
import org.xtreemfs.utils.CLIParser.CliOption;

/**
 *
 * <br>09.06.2009
 */
public class ClusterOSDstart {
    public static final String CONFIGS                      = "c";
    public static final String NUMBER_OF_OSDS               = "n";
    public static final String BREAKOUT_EVERY_X_MIN         = "b";
    public static final String TIMEOUT                      = "t";
    public static final String HELP                         = "h";

    public static final int    DEFAULT_NUMBER_OF_OSDS       = 1;
    public static final int    DEFAULT_BREAKOUT_EVERY_X_MIN = 60;            // breakout every 60min
    public static final int    DEFAULT_TIMEOUT              = 1000 * 60 * 10; // 10min

    private static int         timeout;
    private static Random      random;
    private static int         breakoutEveryXmin;

    public static class OSDBreakout implements Comparable<OSDBreakout> {
        public OSD osd;
        public OSDConfig config;
        public long nextBreakout;
        public long nextWakeup;
        private long nextActionAt;

        @Override
        public int compareTo(OSDBreakout o) {
            return (int) (this.nextActionAt - o.nextActionAt);
        }
        
        public void interrupted() {
            nextBreakout += breakoutEveryXmin;
            nextActionAt = nextWakeup;
        }

        public void wokeUp() {
            nextWakeup = nextBreakout + timeout;
            nextActionAt = nextBreakout;
        }
    }
    
    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        List<String> arguments = new ArrayList<String>(3);
        options.put(CONFIGS, new CliOption(CliOption.OPTIONTYPE.STRING));
        options.put(NUMBER_OF_OSDS, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(BREAKOUT_EVERY_X_MIN, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(TIMEOUT, new CliOption(CliOption.OPTIONTYPE.NUMBER));
        options.put(HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH));
        
        try {
            CLIParser.parseCLI(args, options, arguments);
        } catch (Exception exc) {
            System.out.println(exc);
            usage();
            return;
        }
        
        CliOption h = options.get(HELP);
        if (h.switchValue) {
            usage();
            return;
        }

        // initialize params
        String[] configFiles = null;
        int numberOfOSDs = DEFAULT_NUMBER_OF_OSDS;
        breakoutEveryXmin = DEFAULT_BREAKOUT_EVERY_X_MIN * 1000 * 60;
        timeout = DEFAULT_TIMEOUT;

        // fill params
        for (Entry<String, CliOption> e : options.entrySet()) {
            if (e.getKey().equals(CONFIGS) && e.getValue().stringValue != null) { // mandatory argument
                configFiles = e.getValue().stringValue.split(",");
                
                if (e.getKey().equals(NUMBER_OF_OSDS) && e.getValue().numValue != null) {
                    numberOfOSDs = e.getValue().numValue.intValue();
                } else if (e.getKey().equals(BREAKOUT_EVERY_X_MIN) && e.getValue().numValue != null) {
                    breakoutEveryXmin = e.getValue().numValue.intValue() * 1000 * 60;
                } else if (e.getKey().equals(TIMEOUT) && e.getValue().numValue != null) {
                    timeout = e.getValue().numValue.intValue();
                } else if (e.getKey().equals(HELP) && e.getValue().switchValue) {
                    usage();
                    return;
                }
            } else {
                usage();
                return;
            }
        }

        Random random = new Random();

        Queue<OSDBreakout> osds = new PriorityQueue<OSDBreakout>();
        // start OSDs
        for(int osdIndex=0; osdIndex < numberOfOSDs; osdIndex++){
            OSDBreakout osdBreakout = new OSDBreakout();
            osdBreakout.nextBreakout = System.currentTimeMillis() + random.nextInt(breakoutEveryXmin);
            osdBreakout.nextWakeup = osdBreakout.nextBreakout + timeout;
            osdBreakout.nextActionAt = osdBreakout.nextBreakout;
            osdBreakout.config = new OSDConfig(configFiles[osdIndex].trim());
            osdBreakout.osd = new OSD(osdBreakout.config);
            osds.add(osdBreakout);
        }

        // main logic
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (breakoutEveryXmin != 0)
                    interruptOSDs(osds);
            } catch (InterruptedException e1) {
                continue;
            }
        }

        // shutdown
        for(OSDBreakout osdBreakout : osds)
            osdBreakout.osd.shutdown();
    }

    /**
     * main logic
     * @throws InterruptedException 
     */
    private static void interruptOSDs(Queue<OSDBreakout> osds) throws InterruptedException {
        // remove from queue
        OSDBreakout osdBreakout = osds.poll();
        
        if(osdBreakout.osd != null) { // shutdown
            osdBreakout.osd.shutdown();
            osdBreakout.osd = null;
            osdBreakout.interrupted();
        } else { // wakeup
            osdBreakout.osd = new OSD(osdBreakout.config);
            osdBreakout.wokeUp();
        }
        // add to queue again
        osds.add(osdBreakout);

        Thread.sleep(osds.peek().nextActionAt - System.currentTimeMillis());
    }

    /**
     * 
     */
    private static void usage() {
        StringBuffer out = new StringBuffer();
        out.append("Usage: " + xtfs_repl.class.getSimpleName() + " [options]\n");
        out.append("options:\n");
        out.append("\t-" + CONFIGS + "<config1, config2, ...>: sets the config files for the OSDs\n");
        out.append("\t-" + NUMBER_OF_OSDS + "<number>: sets the number of OSDs on this machine\n");
        out.append("\t-" + BREAKOUT_EVERY_X_MIN + "<number>: sets the ratio of breakouts\n");
        out.append("\t-" + TIMEOUT + "<number>: sets the timeout for the breakout\n");
        out.append("\t-" + HELP + ": shows this help\n");
        System.out.println(out.toString());
    }
}
