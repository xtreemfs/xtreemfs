/*
 * Copyright (c) 2008-2011 by Jan Stender,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.mrc.metadata;


/**
 * Interface for accessing a striping policy. Striping policies may either be
 * part of a file's X-Locations List, or default striping policies assigned for
 * directories, which will be assigned to newly created files.
 */
public interface StripingPolicy {
    
    /**
     * Returns the striping pattern.
     * 
     * @return the striping pattern
     */
    public String getPattern();
    
    /**
     * Returns the striping width, i.e. number of OSDs used for the pattern.
     * 
     * @return the striping width
     */
    public int getWidth();
    
    /**
     * Returns the stripe size, i.e. size of a single object in kBytes.
     * 
     * @return the stripe size
     */
    public int getStripeSize();
    
    /**
     * Returns the parity width, i.e. number of OSDs used for the pattern.
     * 
     * @return the parity width
     */
    public int getParityWidth();

}