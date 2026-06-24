package com.company.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Boots the full application context against real PostgreSQL and Redis containers, exercising Flyway
 * migrations, JPA, Redis, Security and OpenAPI auto-configuration end to end.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class EcommerceApplicationTests {

    @Autowired private ApplicationContext context;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }
}