package org.eclipse.dirigible.components.api.redis;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class RedisFacadeTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:5.0.3-alpine")).withExposedPorts(6379);

    @BeforeAll
    static void setUp() {
        Configuration.set("DIRIGIBLE_REDIS_CLIENT_URI", redis.getHost() + ":" + redis.getFirstMappedPort());
    }

    @Test
    public void getClient() {
        Jedis client = RedisFacade.getClient();
        client.set("key", "value");
        assertEquals("value", client.get("key"));
    }
}
