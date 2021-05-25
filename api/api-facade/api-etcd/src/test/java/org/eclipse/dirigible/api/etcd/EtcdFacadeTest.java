/*
 * Copyright (c) 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2010-2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.api.etcd;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.launcher.EtcdCluster;
import io.etcd.jetcd.test.EtcdClusterExtension;

import com.google.common.base.Charsets;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class EtcdFacadeTest {

	@RegisterExtension static final EtcdCluster etcd = new EtcdClusterExtension("test-etcd", 1);

	@Before
	public void setUp() {
		etcd.start();
		Configuration.set("DIRIGIBLE_ETCD_CLIENT_ENDPOINT", etcd.getClientEndpoints().get(0).toString());
	}

	@Test
	public void getClient() throws IOException, ExecutionException, InterruptedException {
		KV etcdClient = EtcdFacade.getClient();
		assertNotNull(etcdClient);

		ByteSequence key = ByteSequence.from("foo", Charsets.UTF_8);
		ByteSequence value = ByteSequence.from("bar", Charsets.UTF_8);

		etcdClient.put(key, value);
		Thread.sleep(5000);

		GetResponse getPutResponse = etcdClient.get(key).get();
		assertEquals(getPutResponse.getKvs().get(0).getValue().toString(Charsets.UTF_8), value.toString(Charsets.UTF_8));

		etcdClient.delete(key);
		Thread.sleep(5000);

		GetResponse getDelResponse = etcdClient.get(key).get();
		assertTrue(getDelResponse.getKvs().isEmpty());
	}

	@Test
	public void stringToByteSequence() throws IOException {
		String s = "foo";
		ByteSequence bs = EtcdFacade.stringToByteSequence(s);

		assertNotNull(bs);
		assertEquals(bs.toString(Charsets.UTF_8), s);
	}

	@Test
	public void byteArrayToByteSequence() throws IOException {
		byte[] arr = {100, 100, 100};
		ByteSequence bs = EtcdFacade.byteArrayToByteSequence(arr);

		assertNotNull(bs);
		assertTrue(Arrays.equals(bs.getBytes(), arr));
	}

	@Test
	public void byteSequenceToString() throws IOException {
		ByteSequence bs = ByteSequence.from("foo", Charsets.UTF_8);
		String s = EtcdFacade.byteSequenceToString(bs);

		assertNotNull(s);
		assertEquals(bs.toString(Charsets.UTF_8), s);
	}
}
