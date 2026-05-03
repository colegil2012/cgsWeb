package com.ua.estore.cgsWeb.config;

import com.ua.estore.cgsWeb.models.shop.Cart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Ensures the `expiresAt` field on carts has a TTL index so guest carts
 * are auto-deleted by Mongo after they expire. User carts have `expiresAt = null`
 * and are therefore never removed by this index.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CartTtlIndexInitializer {

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureIndexes() {
        try {
            // expireAfter = 0 means "delete at the exact time in expiresAt"
            Index ttl = new Index()
                    .on("expiresAt", org.springframework.data.domain.Sort.Direction.ASC)
                    .expire(Duration.ZERO)
                    .named("cart_expiresAt_ttl");

            mongoTemplate.indexOps(Cart.class).createIndex(ttl);
            log.info("Ensured TTL index on carts.expiresAt");
        } catch (Exception ex) {
            log.warn("Failed to ensure TTL index on carts.expiresAt: {}", ex.getMessage());
        }
    }
}