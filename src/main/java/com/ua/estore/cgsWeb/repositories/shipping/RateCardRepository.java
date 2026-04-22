package com.ua.estore.cgsWeb.repositories.shipping;

import com.ua.estore.cgsWeb.models.shipping.RateCard;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface RateCardRepository extends MongoRepository<RateCard, String> {
    Optional<RateCard> findFirstByActiveTrueOrderByVersionDesc();
}