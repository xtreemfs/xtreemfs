/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.mrc;

import org.xtreemfs.foundation.oncrpc.server.AuthFlavorProvider;
import org.xtreemfs.interfaces.UserCredentials;
import yidl.runtime.Object;

/**
 *
 * @author bjko
 */
public class UserCredentialsAuthFlavorProvider implements AuthFlavorProvider {

    @Override
    public Object getNewAuthObject() {
        return new UserCredentials();
    }

}
