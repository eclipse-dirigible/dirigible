/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.s3;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {org.eclipse.dirigible.components.api.s3.S3Facade.class})
@ComponentScan(basePackages = {"org.eclipse.dirigible.components.*"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class S3FacadeTest {

    private static S3Client s3Client;

    private static final String BUCKET_NAME = "test-cmis-bucket";

    private static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:latest");

    private static LocalStackContainer localstack = new LocalStackContainer(localstackImage).withServices(S3);

    @BeforeAll
    public static void setUp() {
        System.setProperty("aws.accessKeyId", "localstack");
        System.setProperty("aws.secretAccessKey", "localstack");
        System.setProperty("DIRIGIBLE_S3_PROVIDER", "test");
        Configuration.set("DIRIGIBLE_S3_BUCKET", BUCKET_NAME);
        localstack.start();
        S3Facade.setClientForTestContainer(localstack.getEndpoint(), BUCKET_NAME);
        s3Client = S3Client.builder()
                           .endpointOverride(localstack.getEndpoint())
                           .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
                           .region(Region.EU_CENTRAL_1)
                           .build();
    }

    @AfterAll
    public static void tearDown() {
        deleteBucket(BUCKET_NAME);
        localstack.stop();
    }

    @Test
    public void testPutObject() {
        byte[] inputData = "Test data".getBytes();
        String objectKey = "testPutObject";
        String contentType = "text/plain";

        S3Facade.put(objectKey, inputData, contentType);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(BUCKET_NAME)
                                                            .key(objectKey)
                                                            .build();

        GetObjectResponse getObjectResponse = s3Client.getObject(getObjectRequest)
                                                      .response();

        assertEquals(contentType, getObjectResponse.contentType());
        assertEquals(inputData.length, getObjectResponse.contentLength());
    }

    @Test
    public void testGetObject() throws IOException {
        byte[] expectedContent = "Test content".getBytes();
        String objectKey = "getGetObject";
        String contentType = "text/plain";

        S3Facade.put(objectKey, expectedContent, contentType);

        byte[] actualContent = S3Facade.get(objectKey);

        assertArrayEquals(expectedContent, actualContent);
    }

    private static void deleteBucket(String bucketName) {
        ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                                                                      .bucket(bucketName)
                                                                      .build();

        ListObjectsV2Response listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);
        listObjectsResponse.contents()
                           .forEach(object -> {
                               DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                                                                            .bucket(bucketName)
                                                                                            .key(object.key())
                                                                                            .build();
                               s3Client.deleteObject(deleteObjectRequest);
                           });

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .build();
        s3Client.deleteBucket(deleteBucketRequest);
    }
}
