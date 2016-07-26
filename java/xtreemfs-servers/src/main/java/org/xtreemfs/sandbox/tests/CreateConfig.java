//package org.xtreemfs.sandbox.tests;
//
//import java.io.FileOutputStream;
//import java.util.Properties;
//
//public class CreateConfig {
//
//    public static void main(String[] args) {
//
//        try {
//
//            int numCfgFiles = Integer.parseInt(args[0]);
//
//            Properties config = new Properties();
//            if (!args[1].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_STRIPESIZE,
//                    args[1]);
//            if (!args[2].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_NUM_REQUESTS,
//                    args[2]);
//            if (!args[3].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_NUM_OBJECTS,
//                    args[3]);
//            if (!args[4].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_POLICY,
//                    args[4]);
//            if (!args[5].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_FILE_ID,
//                    args[5]);
//            if (!args[6].equals("null"))
//                config.setProperty(OSDTestClient.Configuration.PROP_OPERATION,
//                    args[6]);
//
//            for (int i = 0; i < numCfgFiles; i++)
//                config.setProperty(OSDTestClient.Configuration.PROP_OSD + (i + 1),
//                    "opt" + ((i + 1) < 10 ? ("0" + (i + 1)) : (i + 1))
//                        + ":32640");
//
//            for (int i = 0; i < numCfgFiles; i++) {
//                config.setProperty(OSDTestClient.Configuration.PROP_TARGET_OSD,
//                    (i + 1) + "");
//                if (!args[7].equals("null")) {
//                    long delay = Long.parseLong(args[7]) - i * 1000;
//                    config.setProperty(
//                        OSDTestClient.Configuration.PROP_INIT_DELAY, (delay < 0 ? 0
//                            : delay) + "");
//                }
//
//                config.store(new FileOutputStream("/home/stender/config"
//                    + ((i + 1) < 10 ? ("0" + (i + 1)) : (i + 1))
//                    + ".properties"), "");
//            }
//
//        } catch (Exception ex) {
//            ex.printStackTrace();
//            System.exit(1);
//        }
//
//    }
//}
