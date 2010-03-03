/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils.tunefs;

import java.util.List;
import java.util.Map;
import org.xtreemfs.common.util.CLOptionParser;

/**
 *
 * @author bjko
 */
public interface TuneFSCommand {

    public void addMapping(Map<String,TuneFSCommand> map);

    public String getCommandHelp();

    public void createOptions(CLOptionParser parser);

    public void printUsage(String executableName);

    public void execute(List<String> arguments) throws Exception;

}
