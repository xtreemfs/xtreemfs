/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import org.xtreemfs.babudb.api.BabuDB;
import org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest;

import com.google.protobuf.Message;

/**
 * <p>Processing that is done after a {@link BabuDB} update has been successful.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/21/11
 */
public abstract class BabuDBPostprocessing<T> {
    
    public abstract Message execute(T result, BabuDBRequest request) throws Exception;
    
    public void requeue(BabuDBRequest request) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * <p>BabuDBPostprocessing implementation that simply ignores all incoming calls.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/21/11
     */
    final static class NullPostprocessing extends BabuDBPostprocessing<Object> {
        
        /**
         * <p>Static instance of NullCallback to avoid memory leaks due multiple instances of this type.</p>
         */
        final static BabuDBPostprocessing<Object> INSTANCE = new NullPostprocessing();

        /**
         * <p>Hidden default constructor of this class.</p>
         */
        private NullPostprocessing() { }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.BabuDBPostprocessing#execute(java.lang.Object, 
         *              org.xtreemfs.common.stage.BabuDBComponent.BabuDBRequest)
         */
        @Override
        public Message execute(Object result, BabuDBRequest request) throws Exception {
            return (Message) new Object();
        }
    }
}