/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.server;

import yidl.runtime.Object;

/**
 *
 * @author bjko
 */
public class NullAuthFlavorProvider implements AuthFlavorProvider {

    @Override
    public Object getNewAuthObject() {
        return null;
    }


}
