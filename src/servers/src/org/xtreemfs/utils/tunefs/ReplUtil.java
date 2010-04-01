/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.utils.tunefs;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.util.CLOption;
import org.xtreemfs.foundation.util.InvalidUsageException;
import org.xtreemfs.foundation.oncrpc.utils.XDRUtils;
import org.xtreemfs.utils.utils;
import org.xtreemfs.utils.xtfs_repl;

/**
 *
 * @author bjko
 */
public class ReplUtil {


    static xtfs_repl initRepl(String filePath, CLOption.StringValue pkcsPath, CLOption.StringValue pkcsPass) throws Exception {
        final String localPath = utils.expandPath(filePath);
        final String url = utils.getxattr(localPath, "xtreemfs.url");

        if (url == null) {
            throw new IOException("'" + localPath
                    + "' is probably not part of an XtreemFS volume (no MRC URL found).");
        }

        final int i0 = url.indexOf("://") + 2;
        final int i1 = url.indexOf(':', i0);
        final int i2 = url.indexOf('/', i1);
        final int i3 = url.indexOf('/', i2 + 1);

        final String dirURL = url.substring(i0 + 1, i1);
        final int dirPort = Integer.parseInt(url.substring(i1 + 1, i2));
        final String volName = url.substring(i2 + 1, i3 == -1 ? url.length() : i3);
        final String volPath = i3 == -1 ? "" : url.substring(i3);
        final InetSocketAddress dirAddress = new InetSocketAddress(dirURL, dirPort);

        // create SSL options (if set)
        SSLOptions sslOptions = null;

        if (url.startsWith(XDRUtils.ONCRPCG_SCHEME) || url.startsWith(XDRUtils.ONCRPCS_SCHEME)) {
            if (!pkcsPath.isSet())
                throw new InvalidUsageException("must specify a PCKS#12 file with credentials for (grid)SSL mode, use "+pkcsPath.getName());
            if (!pkcsPass.isSet())
                throw new InvalidUsageException("must specify a PCKS#12 passphrase for (grid)SSL mode, use "+pkcsPass.getName());

            final boolean gridSSL = url.startsWith(XDRUtils.ONCRPCG_SCHEME);
            sslOptions = new SSLOptions(new FileInputStream(pkcsPath.getValue()),pkcsPass.getValue(),"PKCS12",
                    null, null, "none", false, gridSSL);
        }

        return new xtfs_repl(localPath, dirAddress, volName, volPath, sslOptions);
    }
}
