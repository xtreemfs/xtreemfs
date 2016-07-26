/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.osd.rwre;

import java.io.IOException;

/**
 *
 * @author bjko
 */
public class RetryException extends IOException {

    public RetryException(String message) {
        super(message);
    }

}
