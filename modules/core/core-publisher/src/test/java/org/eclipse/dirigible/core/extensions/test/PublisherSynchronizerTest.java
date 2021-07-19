/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.core.extensions.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

import javax.inject.Inject;

import org.eclipse.dirigible.core.publisher.api.IPublisherCoreService;
import org.eclipse.dirigible.core.publisher.api.PublisherException;
import org.eclipse.dirigible.core.publisher.service.PublisherCoreService;
import org.eclipse.dirigible.core.publisher.synchronizer.PublisherSynchronizer;
import org.eclipse.dirigible.core.test.AbstractGuiceTest;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.junit.Before;
import org.junit.Test;

// TODO: Auto-generated Javadoc
/**
 * The Class PublisherSynchronizerTest.
 */
public class PublisherSynchronizerTest extends AbstractGuiceTest {

	/** The publisher core service. */
	@Inject
	private IPublisherCoreService publisherCoreService;

	/** The publisher synchronizer. */
	@Inject
	private PublisherSynchronizer publisherSynchronizer;

	/** The repository. */
	@Inject
	private IRepository repository;

	/**
	 * Sets the up.
	 *
	 * @throws Exception the exception
	 */
	@Before
	public void setUp() throws Exception {
		this.publisherCoreService = getInjector().getInstance(PublisherCoreService.class);
		this.publisherSynchronizer = getInjector().getInstance(PublisherSynchronizer.class);
		this.repository = getInjector().getInstance(IRepository.class);
	}

	/**
	 * Publish resource test.
	 *
	 * @throws PublisherException the publisher exception
	 */
	@Test
	public void publishResourceTest() throws PublisherException {

		repository.createResource("/user1/workspace1/project1/folder1/file.txt", "My Data".getBytes());

		publisherCoreService.createPublishRequest("/user1/workspace1", "/project1/folder1/file.txt", null);

		Timestamp before = publisherCoreService.getLatestPublishLog();

		publisherSynchronizer.synchronize();

		IResource publishedResource = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/project1/folder1/file.txt");

		assertTrue(publishedResource.exists());
		assertEquals("My Data", new String(publishedResource.getContent(), StandardCharsets.UTF_8));

		Timestamp after = publisherCoreService.getLatestPublishLog();

		assertTrue(after.after(before));
	}

	/**
	 * Publish resource twice test.
	 *
	 * @throws PublisherException the publisher exception
	 */
	@Test
	public void publishResourceTwiceTest() throws PublisherException {

		publishResourceTest();

		IResource resource = repository.getResource("/user1/workspace1/project1/folder1/file.txt");
		resource.setContent("My Data 2".getBytes());

		publisherCoreService.createPublishRequest("/user1/workspace1", "/project1/folder1/file.txt", null);

		Timestamp before = publisherCoreService.getLatestPublishLog();

		publisherSynchronizer.synchronize();

		IResource publishedResource = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/project1/folder1/file.txt");

		assertTrue(publishedResource.exists());
		assertEquals("My Data 2", new String(publishedResource.getContent()));

		Timestamp after = publisherCoreService.getLatestPublishLog();

		assertTrue(after.after(before));
		assertFalse(before.equals(new Timestamp(0)));
	}

}
