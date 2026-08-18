/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.cognito;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CognitoSrp}. The round-trip test plays the server side of the SRP protocol
 * (verifier from the registered password, ephemeral B, shared secret S = (A * v^u)^b) with an
 * independent transcription of the math, so a canonicalization slip on either side of the client
 * implementation fails the key comparison.
 */
class CognitoSrpTest {

    private static final BigInteger N = CognitoSrp.N;
    private static final BigInteger G = BigInteger.valueOf(2);

    private static final String POOL_NAME = "TestPool1";
    private static final String USER_ID = "3b3f18f4-1c3d-4b6e-9f9a-0e2f4c1a5d77";
    private static final String PASSWORD = "correct horse battery staple";

    @Test
    void clientAndServerDeriveTheSameKey() throws Exception {
        SecureRandom random = new SecureRandom();
        CognitoSrp client = new CognitoSrp(random);
        BigInteger bigA = new BigInteger(client.srpA(), 16);

        byte[] saltBytes = new byte[16];
        random.nextBytes(saltBytes);
        BigInteger salt = new BigInteger(1, saltBytes);

        // server-side registration: the password verifier v = g^x
        BigInteger x = computeX(salt);
        BigInteger v = G.modPow(x, N);

        // server-side ephemeral key pair: B = k*v + g^b
        BigInteger k = computeK();
        BigInteger b = new BigInteger(1024, random).mod(N);
        BigInteger bigB = k.multiply(v)
                           .add(G.modPow(b, N))
                           .mod(N);

        // server-side shared secret: S = (A * v^u)^b
        BigInteger u = hashToInt(bigA.toByteArray(), bigB.toByteArray());
        BigInteger s = bigA.multiply(v.modPow(u, N))
                           .modPow(b, N)
                           .mod(N);
        byte[] serverKey = hkdf(s.toByteArray(), u.toByteArray());

        byte[] clientKey = client.passwordAuthenticationKey(POOL_NAME, USER_ID, PASSWORD, bigB, salt);

        assertArrayEquals(serverKey, clientKey);
    }

    @Test
    void aWrongPasswordDerivesADifferentKey() throws Exception {
        SecureRandom random = new SecureRandom();
        CognitoSrp client = new CognitoSrp(random);
        BigInteger bigA = new BigInteger(client.srpA(), 16);

        BigInteger salt = new BigInteger(1, new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        BigInteger x = computeX(salt);
        BigInteger v = G.modPow(x, N);
        BigInteger k = computeK();
        BigInteger b = new BigInteger(1024, random).mod(N);
        BigInteger bigB = k.multiply(v)
                           .add(G.modPow(b, N))
                           .mod(N);
        BigInteger u = hashToInt(bigA.toByteArray(), bigB.toByteArray());
        BigInteger s = bigA.multiply(v.modPow(u, N))
                           .modPow(b, N)
                           .mod(N);
        byte[] serverKey = hkdf(s.toByteArray(), u.toByteArray());

        byte[] clientKey = client.passwordAuthenticationKey(POOL_NAME, USER_ID, "not the password", bigB, salt);

        assertNotEquals(Base64.getEncoder()
                              .encodeToString(serverKey),
                Base64.getEncoder()
                      .encodeToString(clientKey));
    }

    @Test
    void aZeroServerKeyIsRefused() {
        CognitoSrp client = new CognitoSrp();
        assertThrows(SecurityException.class, () -> client.passwordAuthenticationKey(POOL_NAME, USER_ID, PASSWORD, N, BigInteger.ONE));
    }

    @Test
    void timestampMatchesCognitosExpectedShape() {
        assertEquals("Tue Aug 18 08:32:12 UTC 2026", CognitoSrp.timestamp(Instant.parse("2026-08-18T08:32:12Z")));
        // the day of month is not zero-padded
        assertEquals("Sat Aug 8 01:02:03 UTC 2026", CognitoSrp.timestamp(Instant.parse("2026-08-08T01:02:03Z")));
    }

    @Test
    void secretHashMatchesTheDocumentedComputation() {
        assertEquals("P2LsDP7jcVEDCxd4Nkf43mVmWNwtq/gqVKPQHji+KAM=",
                CognitoSrp.secretHash("client-id", "client-secret", "jane.doe@example.org"));
    }

    @Test
    void passwordClaimSignatureIsTheHmacOverPoolUserSecretBlockAndTimestamp() throws Exception {
        byte[] key = new byte[] {9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 9, 8, 7, 6, 5, 4};
        byte[] secretBlock = new byte[] {42, 42, 42, 42};
        String timestamp = "Tue Aug 18 08:32:12 UTC 2026";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(POOL_NAME.getBytes(StandardCharsets.UTF_8));
        mac.update(USER_ID.getBytes(StandardCharsets.UTF_8));
        mac.update(secretBlock);
        String expected = Base64.getEncoder()
                                .encodeToString(mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8)));

        String actual = CognitoSrp.passwordClaimSignature(key, POOL_NAME, USER_ID, Base64.getEncoder()
                                                                                         .encodeToString(secretBlock),
                timestamp);

        assertEquals(expected, actual);
    }

    private static BigInteger computeX(BigInteger salt) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(POOL_NAME.getBytes(StandardCharsets.UTF_8));
        digest.update(USER_ID.getBytes(StandardCharsets.UTF_8));
        digest.update(":".getBytes(StandardCharsets.UTF_8));
        byte[] userIdPasswordHash = digest.digest(PASSWORD.getBytes(StandardCharsets.UTF_8));
        digest.reset();
        digest.update(salt.toByteArray());
        return new BigInteger(1, digest.digest(userIdPasswordHash));
    }

    private static BigInteger computeK() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(N.toByteArray());
        return new BigInteger(1, digest.digest(G.toByteArray()));
    }

    private static BigInteger hashToInt(byte[] first, byte[] second) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(first);
        return new BigInteger(1, digest.digest(second));
    }

    private static byte[] hkdf(byte[] ikm, byte[] salt) throws Exception {
        Mac extraction = Mac.getInstance("HmacSHA256");
        extraction.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] pseudoRandomKey = extraction.doFinal(ikm);

        Mac expansion = Mac.getInstance("HmacSHA256");
        expansion.init(new SecretKeySpec(pseudoRandomKey, "HmacSHA256"));
        expansion.update("Caldera Derived Key".getBytes(StandardCharsets.UTF_8));
        byte[] block = expansion.doFinal(new byte[] {1});

        byte[] key = new byte[16];
        System.arraycopy(block, 0, key, 0, 16);
        return key;
    }
}
