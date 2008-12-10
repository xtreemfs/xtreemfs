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
 * AUTHORS: Christian Lorenz (ZIB)
 */
package org.xtreemfs.common.checksums;

import java.util.Collection;
import java.util.HashMap;

/**
 * An abstract class which must be implemented by a checksum provider for
 * XtreemFS.
 *
 * 19.08.2008
 *
 * @author clorenz
 */
public abstract class ChecksumProvider {
	/**
	 * contains the supported algorithms
	 */
	protected HashMap<String, ChecksumAlgorithm> algorithms;

	protected ChecksumProvider() {
		super();
		this.algorithms = new HashMap<String, ChecksumAlgorithm>();
	}

	/**
	 * Returns the name of the provider.
	 *
	 * @return name
	 */
	public abstract String getName();

	/**
	 * Returns all from this provider supported checksum algorithms.
	 *
	 * @return a collection with ChecksumAlgorithms
	 */
	public Collection<ChecksumAlgorithm> getSupportedAlgorithms() {
		return algorithms.values();
	}

	/**
	 * adds an algorithm to the map
	 *
	 * @param newAlgorithm
	 */
	protected void addAlgorithm(ChecksumAlgorithm newAlgorithm) {
		this.algorithms.put(newAlgorithm.getName(), newAlgorithm);
	}
}
