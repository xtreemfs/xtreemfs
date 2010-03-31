/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.client;

import java.io.IOException;
import org.xtreemfs.foundation.oncrpc.client.RemoteExceptionParser;
import org.xtreemfs.foundation.oncrpc.utils.XDRUnmarshaller;
import org.xtreemfs.interfaces.OSDInterface.OSDInterface;
import org.xtreemfs.interfaces.utils.ONCRPCException;

/**
 *
 * @author bjko
 */
public class OSDInterfaceExceptionParser extends RemoteExceptionParser {

    public OSDInterfaceExceptionParser() {

    }

    @Override
    public boolean canParseException(int accept_stat) {
        return (accept_stat >= OSDInterface.getVersion() && (accept_stat < OSDInterface.getVersion()+100));
    }

    @Override
    public ONCRPCException parseException(int accept_stat, XDRUnmarshaller unmarshaller) throws IOException {
        try {
            ONCRPCException ex = OSDInterface.createException(accept_stat);
            ex.unmarshal(unmarshaller);
            return ex;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

}
