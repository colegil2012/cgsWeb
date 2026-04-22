package com.ua.estore.cgsWeb.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import java.util.Date;

/**
 * Simple Mongo-backed implementation of {@link PersistentTokenRepository} for remember-me tokens.
 * Collection: persistent_logins
 */
public class MongoPersistentTokenRepository implements PersistentTokenRepository {

    private final MongoTemplate mongoTemplate;

    public MongoPersistentTokenRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogin doc = new PersistentLogin(
                token.getSeries(), token.getUsername(), token.getTokenValue(), token.getDate());
        mongoTemplate.save(doc);
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(series)),
                new Update().set("token", tokenValue).set("lastUsed", lastUsed),
                PersistentLogin.class
        );
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        PersistentLogin found = mongoTemplate.findById(seriesId, PersistentLogin.class);
        if (found == null) return null;
        return new PersistentRememberMeToken(
                found.getUsername(), found.getSeries(), found.getToken(), found.getLastUsed());
    }

    @Override
    public void removeUserTokens(String username) {
        mongoTemplate.remove(Query.query(Criteria.where("username").is(username)), PersistentLogin.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Document(collection = "persistent_logins")
    public static class PersistentLogin {
        @Id
        private String series;

        @Indexed
        private String username;

        private String token;
        private Date lastUsed;
    }
}