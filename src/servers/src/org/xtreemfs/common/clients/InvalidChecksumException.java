/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xtreemfs.common.clients;

import java.io.IOException;

/**
 *
 * @author bjko
 */
public class InvalidChecksumException extends IOException {

    public InvalidChecksumException(String message) {
        super(message);
    }

}
