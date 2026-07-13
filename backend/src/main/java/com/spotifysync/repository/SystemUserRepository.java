package com.spotifysync.repository;

import com.spotifysync.entity.SystemUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SystemUserRepository extends MongoRepository<SystemUser, String> {
    Optional<SystemUser> findByUsername(String username);
}
