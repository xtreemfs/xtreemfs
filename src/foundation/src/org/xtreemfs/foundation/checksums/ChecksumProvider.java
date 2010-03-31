/*
 * Copyright (c) 2008-2010, Konrad-Zuse-Zentrum fuer Informationstechnik Berlin
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this 
 * list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * Neither the name of the Konrad-Zuse-Zentrum fuer Informationstechnik Berlin 
 * nor the names of its contributors may be used to endorse or promote products 
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * AUTHORS: Christian Lorenz (ZIB)
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
