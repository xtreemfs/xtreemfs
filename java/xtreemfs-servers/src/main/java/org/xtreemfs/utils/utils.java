/*
 * Copyright (c) 2009-2011 by Bjoern Kolbeck, Jan Stender,
 *                            Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.utils;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.xtreemfs.foundation.util.CLIParser.CliOption;

/**
 * 
 * @author bjko
 */
public class utils {
    
    public static final String OPTION_USER_CREDS_FILE = "c";
    
    public static final String OPTION_USER_CREDS_PASS = "cpass";
    
    public static final String OPTION_TRUSTSTORE_FILE = "t";
    
    public static final String OPTION_TRUSTSTORE_PASS = "tpass";
    
    public static final String OPTION_SSL_PROTOCOL    = "-ssl-protocol";
    
    public static final String OPTION_HELP            = "h";
    
    public static final String OPTION_HELP_LONG       = "-help";
    
    public static final String OPTION_ADMIN_PASS      = "-admin_password";
    
    public static Map<String, String> getxattrs(String filename) throws IOException, InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "getfattr", "-m", "xtreemfs.*", "-d", f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0)
            return null;
        
        Map<String, String> result = new HashMap<String, String>();
        
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        br.readLine(); // skip first line
        
        for (;;) {
            String nextLine = br.readLine();
            if (nextLine == null)
                break;
            StringTokenizer st = new StringTokenizer(nextLine, "=");
            if (!st.hasMoreElements())
                continue;
            
            String key = st.nextToken();
            String value = st.nextToken();
            
            // remove leading and trailing quotes
            value = value.substring(1, value.length() - 1);
            value = value.replace("\\\"", "\"");
            
            result.put(key, value);
        }
        
        return result;
    }
    
    public static String getxattr(String filename, String attrname) throws IOException, InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "getfattr", "--only-values", "-n", attrname, f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0)
            return null;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String target = br.readLine();
        
        return target;
    }
    
    public static void setxattr(String filename, String attrname, String attrvalue) throws IOException,
        InterruptedException {
        
        File f = new File(filename);
        Process p = Runtime.getRuntime().exec(
            new String[] { "setfattr", "-n", attrname, "-v", attrvalue, f.getAbsolutePath() });
        p.waitFor();
        if (p.exitValue() != 0) {
            String err = "a problem occurred when setting '" + attrname + "'\n";
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            for (;;) {
                String line = in.readLine();
                if (line == null)
                    break;
                
                err += line + "\n";
            }
            throw new IOException(err);
        }
    }
    
    public static String expandPath(String path) {
        File f = new File(path);
        return f.getAbsolutePath();
    }
    
    public static boolean isXtreemFSDir(String path) throws IOException, InterruptedException {
        String url = getxattr(path, "xtreemfs.url");
        return url != null;
    }
    
    public static String findXtreemFSRootDir(String path) throws IOException, InterruptedException {
        
        path = expandPath(path);
        
        String url = getxattr(path, "xtreemfs.url");
        if (url == null)
            return null;
        
        String parentDir = path.substring(0, path.lastIndexOf(File.separator));
        String xtfsParent = findXtreemFSRootDir(parentDir);
        if (xtfsParent == null)
            return path;
        else
            return xtfsParent;
    }
    
    public static Map<String, CliOption> getDefaultAdminToolOptions(boolean adminPass) {
        
        Map<String, CliOption> options = new HashMap<String, CliOption>();
        options.put(OPTION_USER_CREDS_FILE, new CliOption(CliOption.OPTIONTYPE.STRING,
            "a PKCS#12 file containing user credentials (SSL/GridSSL only)", "<creds_file>"));
        options.put(OPTION_USER_CREDS_PASS,
            new CliOption(CliOption.OPTIONTYPE.STRING,
                "a pass phrase to decrypt the the user credentials file (SSL/GridSSL only).  Set to '-' to prompt for the passphrase.",
                "<creds_passphrase>"));
        options.put(OPTION_TRUSTSTORE_FILE, new CliOption(CliOption.OPTIONTYPE.STRING,
            "a PKCS#12 file containing a set of certificates from trusted CAs (SSL/GridSSL only)",
            "<trusted_CAs>"));
        options.put(OPTION_TRUSTSTORE_PASS, new CliOption(CliOption.OPTIONTYPE.STRING,
            "a pass phrase to decrypt the trusted CAs file (SSL/GridSSL only).  Set to '-' to prompt for the passphrase.",
            "<trusted_passphrase>"));
        options.put(OPTION_SSL_PROTOCOL, new CliOption(CliOption.OPTIONTYPE.STRING,
            "SSL/TLS version to use: ssltls, tlsv1, tlsv11, tlsv12. 'ssltls' (default) accepts all versions, " + 
            "the others accept only the exact version they name. 'tlsv12' is available in JDK 7+ only. " + 
            "'tlsv11' comes with JDK 6 or 7, depending on the vendor.",
            "<ssl_protocol>"));
        options.put(OPTION_HELP, new CliOption(CliOption.OPTIONTYPE.SWITCH, "show usage information", ""));
        options.put(OPTION_HELP_LONG,
            new CliOption(CliOption.OPTIONTYPE.SWITCH, "show usage information", ""));
        if (adminPass)
            options.put(OPTION_ADMIN_PASS, new CliOption(CliOption.OPTIONTYPE.STRING,
                "administrator password to authorize operation. Set to '-' to prompt for the passphrase.", "<passphrase>"));
        
        return options;
    }
    
    public static void printOptions(Map<String, CliOption> options) {
        
        // sort all options by name
        Map<String, CliOption> optionsMap = new TreeMap<String, CliOption>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                if (o1.startsWith("-") && !o2.startsWith("-"))
                    return 1;
                if (!o1.startsWith("-") && o2.startsWith("-"))
                    return -1;
                return o1.compareTo(o2);
            }
        });
        optionsMap.putAll(options);
        
        int maxLength = 0;
        for (Entry<String, CliOption> entry : optionsMap.entrySet()) {
            int len = entry.getKey().length() + entry.getValue().usageParams.length();
            if (len > maxLength)
                maxLength = len;
        }
        
        Iterator<Entry<String, CliOption>> it = optionsMap.entrySet().iterator();
        
        String previous = "";
        while (it.hasNext()) {
            
            Entry<String, CliOption> next = it.next();
            if (next.getKey().startsWith("-") && !previous.startsWith("-"))
                System.out.println();
            
            StringBuffer line = new StringBuffer();
            line.append("      " + "-" + next.getKey() + " " + next.getValue().usageParams);
            for (int i = 0; i < maxLength - next.getKey().length() - next.getValue().usageParams.length() + 4; i++)
                line.append(" ");
            line.append(next.getValue().usageText);
            
            System.out.println(line.toString());
            
            previous = next.getKey();
        }
    }

    public static String readPassword(String format, Object... args) {
        Console console = System.console();
        if (console != null) {
            return new String(console.readPassword(format, args));
        } else {
            // non-interactive console, e.g. from a cron
            // this will not hide the typed characters though,
            // so use as fallback only.
            System.out.println(String.format(format, args));
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                String password = reader.readLine();
                reader.close();
                return password;
            } catch (IOException e) {
                return null;
            }
        }
    }
}
