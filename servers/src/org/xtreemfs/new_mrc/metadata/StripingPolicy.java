/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Jan Stender (ZIB)
 */

package org.xtreemfs.new_mrc.metadata;


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
    
}