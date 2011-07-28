/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.foundation.checksums;

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
