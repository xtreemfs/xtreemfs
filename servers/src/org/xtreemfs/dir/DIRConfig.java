/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

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
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.dir;

import java.io.IOException;
import java.util.Properties;

import org.xtreemfs.common.config.ServiceConfig;

/**
 *
 * @author bjko
 */
public class DIRConfig extends ServiceConfig {

    private String dbDir;

    private String authenticationProvider;

    

    /** Creates a new instance of OSDConfig */
    public DIRConfig(String filename) throws IOException {
        super(filename);
        read();
    }

    public DIRConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }

    public void read() throws IOException {
        super.read();

        this.dbDir = this.readRequiredString("database.dir");
        this.authenticationProvider = readRequiredString("authentication_provider");      

    }

    public String getDbDir() {
        return dbDir;
    }

    public String getAuthenticationProvider() {
        return authenticationProvider;
    }


}
