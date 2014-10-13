/*
 * Copyright (c) 2008-2014 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.contrib.provisioning;

import org.xtreemfs.common.config.ServiceConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class IRMConfig extends ServiceConfig {
    final Parameter[] irmParameter = {
            Parameter.DEBUG_LEVEL,
            Parameter.DEBUG_CATEGORIES,
            Parameter.PORT,
            Parameter.LISTEN_ADDRESS,
            Parameter.HOSTNAME,
            Parameter.DIRECTORY_SERVICE,
            Parameter.SCHEDULER_SERVICE,
            Parameter.USE_SSL,
            Parameter.SERVICE_CREDS_FILE,
            Parameter.SERVICE_CREDS_PASSPHRASE,
            Parameter.SERVICE_CREDS_CONTAINER,
            Parameter.TRUSTED_CERTS_FILE,
            Parameter.TRUSTED_CERTS_CONTAINER,
            Parameter.TRUSTED_CERTS_PASSPHRASE,
            Parameter.TRUST_MANAGER,
            Parameter.USE_GRID_SSL_MODE,
            Parameter.UUID,
            Parameter.WAIT_FOR_DIR,
            Parameter.GEO_COORDINATES,
            Parameter.ADMIN_PASSWORD,
            Parameter.CRS_URL,
            Parameter.SEQ_COST,
            Parameter.RANDOM_COST,
            Parameter.CAPACITY_COST
    };

    /** Creates a new instance of MRCConfig */
    public IRMConfig(String filename) throws IOException {
        super(filename);
        read();
    }

    public IRMConfig(Properties prop) throws IOException {
        super(prop);
        read();
    }

    public IRMConfig(HashMap<String, String> hm) {
        super(hm);
    }

    public void read() throws IOException {

        for (Parameter parm : irmParameter) {
            parameter.put(parm, readParameter(parm));
        }
    }

    public void setDefaults() {
        super.setDefaults(irmParameter);
    }

}
