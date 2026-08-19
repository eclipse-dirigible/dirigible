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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The client side of Cognito's {@code USER_SRP_AUTH} flow - one instance per authentication
 * attempt, holding the ephemeral SRP key pair. Pure JCA against Cognito's published group
 * parameters (the RFC 5054 3072-bit group, g = 2), mirroring AWS's reference implementation
 * byte-for-byte, since the server derives the same values from the same canonicalization
 * ({@link BigInteger#toByteArray()} of every intermediate).
 *
 * <p>
 * With SRP the password is used only to compute a zero-knowledge proof, so it never crosses the
 * wire from the platform to AWS.
 */
class CognitoSrp {

    /** The RFC 5054 3072-bit group prime, as published for Cognito SRP. */
    private static final String HEX_N = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74"
            + "020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B302B0A6DF25F1437"
            + "4FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF05"
            + "98DA48361C55D39A69163FA8FD24CF5F83655D23DCA3AD961C62F356208552BB"
            + "9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
            + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF695581718"
            + "3995497CEA956AE515D2261898FA051015728E5A8AAAC42DAD33170D04507A33"
            + "A85521ABDF1CBA64ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
            + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6BF12FFA06D98A0864"
            + "D87602733EC86A64521F2B18177B200CBBE117577A615D6C770988C0BAD946E2"
            + "08E24FA074E5AB3143DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF";

    static final BigInteger N = new BigInteger(HEX_N, 16);
    private static final BigInteger G = BigInteger.valueOf(2);

    /** The SRP multiplier parameter k = H(N | g). */
    private static final BigInteger K;

    private static final int EPHEMERAL_KEY_LENGTH_BITS = 1024;
    private static final int DERIVED_KEY_SIZE_BYTES = 16;
    private static final String DERIVED_KEY_INFO = "Caldera Derived Key";

    private static final String HMAC_SHA_256 = "HmacSHA256";

    /** Cognito's expected password-claim timestamp shape, e.g. {@code Tue Aug 18 08:32:12 UTC 2026}. */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss 'UTC' yyyy", Locale.US)
                                                                               .withZone(ZoneOffset.UTC);

    static {
        MessageDigest digest = sha256();
        digest.update(N.toByteArray());
        K = new BigInteger(1, digest.digest(G.toByteArray()));
    }

    /** The ephemeral SRP private key. */
    private final BigInteger a;

    /** The ephemeral SRP public key A = g^a mod N. */
    private final BigInteger bigA;

    CognitoSrp() {
        this(new SecureRandom());
    }

    CognitoSrp(SecureRandom random) {
        BigInteger generatedA;
        BigInteger generatedBigA;
        do {
            generatedA = new BigInteger(EPHEMERAL_KEY_LENGTH_BITS, random).mod(N);
            generatedBigA = G.modPow(generatedA, N);
        } while (generatedBigA.mod(N)
                              .equals(BigInteger.ZERO));
        this.a = generatedA;
        this.bigA = generatedBigA;
    }

    /**
     * The {@code SRP_A} auth parameter for {@code InitiateAuth}.
     *
     * @return the ephemeral public key as hex
     */
    String srpA() {
        return bigA.toString(16);
    }

    /**
     * Derives the password authentication key for the {@code PASSWORD_VERIFIER} challenge.
     *
     * @param poolName the user pool name (the pool id after the region prefix)
     * @param userIdForSrp the {@code USER_ID_FOR_SRP} challenge parameter
     * @param password the user's password
     * @param srpB the server's ephemeral public key ({@code SRP_B}, hex-decoded)
     * @param salt the user's salt ({@code SALT}, hex-decoded)
     * @return the 16-byte derived key the password claim is signed with
     */
    byte[] passwordAuthenticationKey(String poolName, String userIdForSrp, String password, BigInteger srpB, BigInteger salt) {
        if (srpB.mod(N)
                .equals(BigInteger.ZERO)) {
            throw new SecurityException("SRP error: B cannot be zero");
        }
        MessageDigest digest = sha256();
        digest.update(bigA.toByteArray());
        BigInteger u = new BigInteger(1, digest.digest(srpB.toByteArray()));
        if (u.equals(BigInteger.ZERO)) {
            throw new SecurityException("SRP error: the hash of A and B cannot be zero");
        }

        digest.reset();
        digest.update(poolName.getBytes(StandardCharsets.UTF_8));
        digest.update(userIdForSrp.getBytes(StandardCharsets.UTF_8));
        digest.update(":".getBytes(StandardCharsets.UTF_8));
        byte[] userIdPasswordHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

        digest.reset();
        digest.update(salt.toByteArray());
        BigInteger x = new BigInteger(1, digest.digest(userIdPasswordHash));

        BigInteger s = srpB.subtract(K.multiply(G.modPow(x, N)))
                           .modPow(a.add(u.multiply(x)), N)
                           .mod(N);
        return hkdf(s.toByteArray(), u.toByteArray());
    }

    /**
     * Signs the password claim for the {@code PASSWORD_VERIFIER} challenge response.
     *
     * @param key the derived password authentication key
     * @param poolName the user pool name
     * @param userIdForSrp the {@code USER_ID_FOR_SRP} challenge parameter
     * @param secretBlockBase64 the {@code SECRET_BLOCK} challenge parameter, as received
     * @param timestamp the claim timestamp, from {@link #timestamp(Instant)}
     * @return the Base64-encoded {@code PASSWORD_CLAIM_SIGNATURE}
     */
    static String passwordClaimSignature(byte[] key, String poolName, String userIdForSrp, String secretBlockBase64, String timestamp) {
        Mac mac = hmacSha256(key);
        mac.update(poolName.getBytes(StandardCharsets.UTF_8));
        mac.update(userIdForSrp.getBytes(StandardCharsets.UTF_8));
        mac.update(Base64.getDecoder()
                         .decode(secretBlockBase64));
        return Base64.getEncoder()
                     .encodeToString(mac.doFinal(timestamp.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Computes the {@code SECRET_HASH} auth parameter for app clients with a client secret.
     *
     * @param clientId the app client id
     * @param clientSecret the app client secret
     * @param username the username the parameter accompanies
     * @return the Base64-encoded secret hash
     */
    static String secretHash(String clientId, String clientSecret, String username) {
        Mac mac = hmacSha256(clientSecret.getBytes(StandardCharsets.UTF_8));
        mac.update(username.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder()
                     .encodeToString(mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Formats the password-claim timestamp the way Cognito expects it.
     *
     * @param moment the moment to format
     * @return the formatted timestamp
     */
    static String timestamp(Instant moment) {
        return TIMESTAMP_FORMAT.format(moment);
    }

    private static byte[] hkdf(byte[] ikm, byte[] salt) {
        byte[] pseudoRandomKey = hmacSha256(salt).doFinal(ikm);
        Mac expansion = hmacSha256(pseudoRandomKey);
        expansion.update(DERIVED_KEY_INFO.getBytes(StandardCharsets.UTF_8));
        byte[] block = expansion.doFinal(new byte[] {1});
        byte[] key = new byte[DERIVED_KEY_SIZE_BYTES];
        System.arraycopy(block, 0, key, 0, DERIVED_KEY_SIZE_BYTES);
        return key;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static Mac hmacSha256(byte[] key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key, HMAC_SHA_256));
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException(HMAC_SHA_256 + " is not available", ex);
        }
    }
}
