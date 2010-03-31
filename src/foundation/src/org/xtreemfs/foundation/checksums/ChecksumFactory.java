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

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Factory for getting checksum algorithms from checksum provider. Implemented
 * as a Singleton.
 *
 * 19.08.2008
 *
 * @author clorenz
 */
public class ChecksumFactory {
	/**
	 * amount of cached instances/algorithm
	 */
	private static int MAX_CACHE_SIZE = 20;

	private static ChecksumFactory self;

	/**
	 * Contains all available checksum algorithms (only one instance).
	 */
	private HashMap<String, ChecksumAlgorithm> algorithms;

	/**
	 * Contains all known checksum provider
	 */
	private HashMap<String, ChecksumProvider> knownProvider;

	/**
	 * Contains cached instances for all available checksum algorithms.
	 */
	private HashMap<String, ConcurrentLinkedQueue<ChecksumAlgorithm>> pool;

	/**
	 * creates a new ChecksumFactory
	 */
	private ChecksumFactory() {
		super();
		this.algorithms = new HashMap<String, ChecksumAlgorithm>();
		this.pool = new HashMap<String, ConcurrentLinkedQueue<ChecksumAlgorithm>>();
		this.knownProvider = new HashMap<String, ChecksumProvider>();
	}

	/**
	 * Get the instance of ChecksumFactory.
	 *
	 * @return the instance
	 */
	public static ChecksumFactory getInstance() {
		if (self == null) {
			self = new ChecksumFactory();
		}
		return self;
	}

	/**
	 * Get an instance of a specific checksum algorithm, if supported.
	 *
	 * @param name
	 *            of the algorithm
	 * @return algorithm object or null, if algorithm is not supported
	 */
	public ChecksumAlgorithm getAlgorithm(String name)
			throws NoSuchAlgorithmException {
		ConcurrentLinkedQueue<ChecksumAlgorithm> cache = pool.get(name);
		if (cache == null)
			throw new NoSuchAlgorithmException("algorithm " + name
					+ " not supported");

		ChecksumAlgorithm algorithm = cache.poll();
		if (algorithm == null) { // cache is empty
			return algorithms.get(name).clone(); // create new instance
		} else {
			return algorithm; // return caches instance
		}
	}

	/**
	 * Returns an instance of a specific checksum algorithm for caching.
	 *
	 * @param instance
	 *            of the algorithm
	 */
	public void returnAlgorithm(ChecksumAlgorithm algorithm) {
		ConcurrentLinkedQueue<ChecksumAlgorithm> cache = pool.get(algorithm
				.getName());
		if (cache.size() < MAX_CACHE_SIZE) {
			algorithm.reset();
			cache.add(algorithm);
		}
	}

	/**
	 * Adds a new provider to factory and adds all supported algorithms from the
	 * provider to the algorithms-list. NOTE: Existing algorithms will be
	 * overridden when the new provider contains the same algorithm (maybe
	 * another implementation).
	 *
	 * @param provider
	 */
	public void addProvider(ChecksumProvider provider) {
		knownProvider.put(provider.getName(), provider);
		for (ChecksumAlgorithm algorithm : provider.getSupportedAlgorithms()) {
			addAlgorithm(algorithm);
		}
	}

	/**
	 * Adds a new Algorithm to factory. NOTE: The same existing algorithm will
	 * be overridden.
	 *
	 * @param algorithm
	 */
	public void addAlgorithm(ChecksumAlgorithm algorithm) {
		algorithms.put(algorithm.getName(), algorithm);
		pool.put(algorithm.getName(),
				new ConcurrentLinkedQueue<ChecksumAlgorithm>());
	}

	/**
	 * Removes a provider, but not the added algorithms.
	 *
	 * @param provider
	 */
	public void removeProvider(ChecksumProvider provider) {
		knownProvider.remove(provider.getName());
	}

	/**
	 * Removes an algorithm.
	 *
	 * @param algorithm
	 */
	public void removeAlgorithm(String algorithm) {
		algorithms.remove(algorithm);
		pool.remove(algorithm);
	}
}
