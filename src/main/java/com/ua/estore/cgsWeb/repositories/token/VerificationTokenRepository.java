package com.ua.estore.cgsWeb.repositories.token;

import com.ua.estore.cgsWeb.models.token.TokenType;
import com.ua.estore.cgsWeb.models.token.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    /**
     * Primary validation lookup. The endpoint hashes the raw token from the
     * email link and looks it up here.
     */
    Optional<VerificationToken> findByTokenHash(String tokenHash);

    /**
     * All outstanding tokens of a given type for a user. Used when issuing a
     * new token — any prior unused tokens of the same type are invalidated
     * first, so a user who requests two password resets can't have the older
     * link still work.
     */
    List<VerificationToken> findByUserIdAndType(String userId, TokenType type);
}