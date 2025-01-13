/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.componenets.api.etcd;

import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.api.etcd.EtcdFacade;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Class EtcdFacadeTest.
 */
@WithMockUser
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EtcdFacadeTest {

    /**
     * The Constant etcd.
     */
    @RegisterExtension
    public static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
                                                                           .withNodes(1)
                                                                           .build();

    /**
     * Sets the up.
     */
    @Before
    public void setUp() {
        cluster.restart(-1, TimeUnit.SECONDS);
        Configuration.set("DIRIGIBLE_ETCD_CLIENT_ENDPOINT", cluster.clientEndpoints()
                                                                   .get(0)
                                                                   .toString());
    }

    /**
     * Gets the client.
     *
     * @return the client
     * @throws ExecutionException the execution exception
     * @throws InterruptedException the interrupted exception
     */
    @Test
    public void getClient() throws ExecutionException, InterruptedException {
        KV etcdClient = EtcdFacade.getClient();
        assertNotNull(etcdClient);

        ByteSequence key = ByteSequence.from("foo", Charsets.UTF_8);
        ByteSequence value = ByteSequence.from("bar", Charsets.UTF_8);

        etcdClient.put(key, value);
        Thread.sleep(500);

        GetResponse getPutResponse = etcdClient.get(key)
                                               .get();
        assertEquals(getPutResponse.getKvs()
                                   .get(0)
                                   .getValue()
                                   .toString(Charsets.UTF_8),
                value.toString(Charsets.UTF_8));

        etcdClient.delete(key);
        Thread.sleep(500);

        GetResponse getDelResponse = etcdClient.get(key)
                                               .get();
        assertTrue(getDelResponse.getKvs()
                                 .isEmpty());
    }

    /**
     * String to byte sequence.
     */
    @Test
    public void stringToByteSequence() {
        String s = "foo";
        ByteSequence bs = EtcdFacade.stringToByteSequence(s);

        assertNotNull(bs);
        assertEquals(bs.toString(Charsets.UTF_8), s);
    }

    /**
     * Byte array to byte sequence.
     */
    @Test
    public void byteArrayToByteSequence() {
        byte[] arr = {100, 100, 100};
        ByteSequence bs = EtcdFacade.byteArrayToByteSequence(arr);

        assertNotNull(bs);
        assertArrayEquals(bs.getBytes(), arr);
    }

    /**
     * Byte sequence to string.
     */
    @Test
    public void byteSequenceToString() {
        ByteSequence bs = ByteSequence.from("foo", Charsets.UTF_8);
        String s = EtcdFacade.byteSequenceToString(bs);

        assertNotNull(s);
        assertEquals(bs.toString(Charsets.UTF_8), s);
    }
}
