/*
 * Copyright (c) 2008-2010 by Christian Lorenz,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
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
