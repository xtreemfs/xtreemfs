/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.foundation.oncrpc.client;

import org.xtreemfs.common.buffer.ReusableBuffer;
import org.xtreemfs.interfaces.utils.Serializable;

/**
 *
 * @author bjko
 */
public interface RPCResponseDecoder<V> {

    public V getResult(ReusableBuffer data);

}
