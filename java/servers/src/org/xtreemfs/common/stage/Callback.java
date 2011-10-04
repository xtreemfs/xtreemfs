/*
 * Copyright (c) 2011 by Felix Langner,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */
package org.xtreemfs.common.stage;

import org.xtreemfs.foundation.pbrpc.utils.ErrorUtils.ErrorResponseException;

/**
 * <p>Callback for postprocessing a {@link AugmentedServiceRequest}.</p>
 * 
 * @author fx.langner
 * @version 1.00, 09/03/11
 */
public interface Callback {
    
    /**
     * <p>Method to call if the request connected to this callback was successful.</p>
     * 
     * @param stageRequest - the original request of this processing step. 
     * @param result - for the request.
     * 
     * @return true, if callback execution could process the request successfully. false, if it has not yet been 
     *         finished and will remain at the current processing step.
     *         
     * @throws ErrorResponseException if the request preprocessing provided by this method was not successful.
     */
    public <S extends StageRequest<?>> boolean success(Object result, S stageRequest) throws ErrorResponseException;
    
    /**
     * <p>Method that is called if execution of a request failed because of <code>error</code>.</p>
     * 
     * @param error - reason for the failure.
     */
    public void failed(Throwable error);
    
    /**
     * <p>Callback implementation that simply ignores all incoming calls.</p>
     * 
     * @author fx.langner
     * @version 1.00, 09/13/11
     */
    final static class NullCallback implements Callback {
        
        /**
         * <p>Static instance of NullCallback to avoid memory leaks due multiple instances of this type.</p>
         */
        final static Callback INSTANCE = new NullCallback();

        /**
         * <p>Hidden default constructor of this class.</p>
         */
        private NullCallback() { }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Callback#failed(java.lang.Exception)
         */
        @Override
        public void failed(Throwable error) { /* ignored */ }

        /* (non-Javadoc)
         * @see org.xtreemfs.common.stage.Callback#success(java.lang.Object, org.xtreemfs.common.stage.StageRequest)
         */
        @Override
        public <S extends StageRequest<?>> boolean success(Object result, S stageRequest) 
                throws ErrorResponseException {
            
            return true;
        }
    }
}
