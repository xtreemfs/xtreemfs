/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xtreemfs.new_dir;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import org.xtreemfs.babudb.BabuDB;
import org.xtreemfs.babudb.BabuDBException;
import org.xtreemfs.common.buffer.BufferPool;
import org.xtreemfs.common.logging.Logging;

/**
 *
 * @author bjko
 */
public class StatusPage {

    private final static String statusPageTemplate;

    private enum Vars {

        MAXMEM("<!-- $MAXMEM -->"),
        FREEMEM("<!-- $FREEMEM -->"),
        AVAILPROCS("<!-- $AVAILPROCS -->"),
        BPSTATS("<!-- $BPSTATS -->"),
        PORT("<!-- $PORT -->"),
        DEBUG("<!-- $DEBUG -->"),
        NUMCON("<!-- $NUMCON -->"),
        PINKYQ("<!-- $PINKYQ -->"),
        NUMREQS("<!-- $NUMREQS -->"),
        TIME("<!-- $TIME -->"),
        TABLEDUMP("<!-- $TABLEDUMP -->");

        private String template;

        Vars(String template) {
            this.template = template;
        }

        public String toString() {
            return template;
        }
    };


    static {
        StringBuffer sb = null;
        try {
            InputStream is = StatusPage.class.getClassLoader().getResourceAsStream(
                    "org/xtreemfs/dir/templates/status.html");
            if (is == null) {
                is = StatusPage.class.getClass().getResourceAsStream("../templates/status.html");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            sb = new StringBuffer();
            String line = br.readLine();
            while (line != null) {
                sb.append(line + "\n");
                line = br.readLine();
            }
            br.close();
        } catch (Exception ex) {
            Logging.logMessage(Logging.LEVEL_DEBUG, null, ex);
        }
        if (sb == null) {
            statusPageTemplate = "<H1>Template was not found, unable to show status page!</h1>";
        } else {
            statusPageTemplate = sb.toString();
        }
    }

    public static String getStatusPage(BabuDB database, DIRConfig config) throws BabuDBException {

        assert (statusPageTemplate != null);

        long time = System.currentTimeMillis();

        /*Object[] dbDump = getDBDump();

        Map<String, Map<String, String>> entities = (Map<String, Map<String, String>>) dbDump[0];
        Map<String, Object[]> mappings = (Map<String, Object[]>) dbDump[1];

        StringBuilder dump = new StringBuilder();
        dump.append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Address Mapping</td>");
        dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
        for (String uuid : mappings.keySet()) {
            Object[] entry = mappings.get(uuid);
            List<Map<String, Object>> mapping = (List<Map<String, Object>>) entry[1];

            dump.append("<tr><td class=\"uuid\">");
            dump.append(uuid);
            dump.append("</td><td class=\"dump\"><table width=\"100%\"><tr>");
            dump.append("<tr><td><table width=\"100%\">");
            for (int i = 0; i < mapping.size(); i++) {
                dump.append("<tr><td class=\"mapping\">");
                Map<String, Object> map = mapping.get(i);
                String endpoint = map.get("protocol") + "://" + map.get("address") + ":" + map.get("port");
                dump.append("<a href=\"" + endpoint + "\">");
                dump.append(endpoint);
                dump.append("</a></td><td class=\"mapping\">");
                dump.append(map.get("match_network"));
                dump.append("</td><td class=\"mapping\">");
                dump.append(map.get("ttl"));
                dump.append("</td></tr>");
            }
            dump.append("</table></td></tr>");
            dump.append("<td class=\"version\">version: <b>");
            dump.append(entry[2]);
            dump.append("</b></td></tr></table>");
        }
        dump.append("</td></tr></table>");

        dump.append("<br><table width=\"100%\" frame=\"box\"><td colspan=\"2\" class=\"heading\">Data Mapping</td>");
        dump.append("<tr><td class=\"dumpTitle\">UUID</td><td class=\"dumpTitle\">mapping</td></tr>");
        for (String uuid : entities.keySet()) {
            Map<String, String> entry = entities.get(uuid);
            dump.append("<tr><td class=\"uuid\">");
            dump.append(uuid);
            dump.append("</td><td class=\"dump\"><table width=\"100%\">");
            List<String> keys = new LinkedList<String>(entry.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                if (key.equals("version")) {
                    continue;
                }
                dump.append("<tr><td width=\"30%\">");
                dump.append(key);
                dump.append("</td><td><b>");
                dump.append(entry.get(key));
                if (key.equals("lastUpdated")) {
                    dump.append(" (");
                    dump.append(new Date(Long.parseLong(entry.get(key)) * 1000));
                    dump.append(")");
                } else if (key.equals("free") || key.equals("total") || key.endsWith("RAM")) {
                    dump.append(" bytes (");
                    dump.append(OutputUtils.formatBytes(Long.parseLong(entry.get(key))));
                    dump.append(")");
                } else if (key.equals("load")) {
                    dump.append("%");
                }
                dump.append("</b></td></tr>");
            }
            dump.append("<td></td><td class=\"version\">version: <b>");
            dump.append(entry.get("version"));
            dump.append("</b></td></table></td></tr>");
        }
        dump.append("</table>");
        */
        String tmp = null;
        try {
            tmp = statusPageTemplate.replace(Vars.AVAILPROCS.toString(), Runtime.getRuntime().availableProcessors() + " bytes");
        } catch (Exception e) {
            tmp = statusPageTemplate;
        }
        tmp = tmp.replace(Vars.FREEMEM.toString(), Runtime.getRuntime().freeMemory() + " bytes");
        tmp = tmp.replace(Vars.MAXMEM.toString(), Runtime.getRuntime().maxMemory() + " bytes");
        tmp = tmp.replace(Vars.BPSTATS.toString(), BufferPool.getStatus());
        tmp = tmp.replace(Vars.PORT.toString(), Integer.toString(config.getPort()));
        tmp = tmp.replace(Vars.DEBUG.toString(), Integer.toString(config.getDebugLevel()));
        tmp = tmp.replace(Vars.NUMCON.toString(), "n/a");
        tmp = tmp.replace(Vars.PINKYQ.toString(), "n/a");
        tmp = tmp.replace(Vars.NUMREQS.toString(), "n/a");
        tmp = tmp.replace(Vars.TIME.toString(), new Date(time).toString() + " (" + time + ")");
        tmp = tmp.replace(Vars.TABLEDUMP.toString(), "");

        return tmp;

    }
}
