/*  Copyright (c) 2010 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Björn Kolbeck, Jan Stender (ZIB)
 */

package org.xtreemfs.utils.tunefs;

import java.util.List;
import java.util.Map;

import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.CLOptionParser;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.util.CLOption.StringValue;
import org.xtreemfs.utils.utils;

/**
 * 
 * @author stender
 */
public class ACLCommand implements TuneFSCommand {
    
    private CLOption.StringValue optModify;
    
    private CLOption.StringValue optRemove;
    
    @Override
    public void addMapping(Map<String, TuneFSCommand> map) {
        map.put("acl", this);
    }
    
    @Override
    public String getCommandHelp() {
        return "acl: show/set the access control list (ACL) of a file";
    }
    
    @Override
    public void createOptions(CLOptionParser parser) {
        
        optModify = new CLOption.StringValue("m", "modify", " sets or modifies an ACL entry");
        optModify = (StringValue) parser.addOption(optModify);
        
        optRemove = new CLOption.StringValue("x", "remove", " removes an ACL entry");
        optRemove = (StringValue) parser.addOption(optRemove);
    }
    
    @Override
    public void printUsage(String executableName) {
        System.out.println("acl <path>: lists the ACL of a file");
        System.out.println("acl -m|--modify u|g|m|o:[<name>]:[<rwx>|<octal>] <path>: updates an ACL entry");
        System.out.println("acl -x|--remove u|g|m|o:<name> <path>: removes an ACL entry");
    }
    
    @Override
    public void execute(List<String> arguments) throws Exception {
        
        if (arguments.size() != 1)
            throw new InvalidUsageException("usage: " + TuneFS.EXEC_NAME
                + " acl [-m|-x] [<entity>[:rights]] <path> ...");
        
        final String path = arguments.get(0);
        
        if (optModify.getValue() != null)
            utils.setxattr(path, "xtreemfs.acl", "m " + optModify.getValue());
        
        if (optRemove.getValue() != null)
            utils.setxattr(path, "xtreemfs.acl", "x " + optRemove.getValue());
        
        if (optModify.getValue() == null && optRemove.getValue() == null)
            System.out.println(utils.getxattr(path, "xtreemfs.acl"));
    }
    
}
