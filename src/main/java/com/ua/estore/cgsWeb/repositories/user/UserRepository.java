package com.ua.estore.cgsWeb.repositories.user;

import com.ua.estore.cgsWeb.models.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);

    Page<User> findAll(Pageable pageable);
    Page<User> findByRolesContaining(String role, Pageable pageable);

    long countByRolesContaining(String role);
    long countByRolesContainingAndEnabledTrue(String role);

    // "Who manages this vendor?" — shown on the vendor detail page.
    List<User> findByVendorId(String vendorId);

    // Admin user-search modal (assign-user-to-vendor flow). Case-insensitive
    // match against username OR email. Profile-name search is handled in the
    // service by also scanning the returned set, OR add the @Query below.
    @Query("{ $or: [ "
            + "{ 'username': { $regex: ?0, $options: 'i' } }, "
            + "{ 'email': { $regex: ?0, $options: 'i' } }, "
            + "{ 'profile.firstName': { $regex: ?0, $options: 'i' } }, "
            + "{ 'profile.lastName': { $regex: ?0, $options: 'i' } } "
            + "] }")
    List<User> searchByNameOrEmail(String regex);

}
