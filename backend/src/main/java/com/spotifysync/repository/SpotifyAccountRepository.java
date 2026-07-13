package com.spotifysync.repository;

import com.spotifysync.entity.SpotifyAccount;
import com.spotifysync.enums.AccountType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SpotifyAccountRepository extends MongoRepository<SpotifyAccount, String> {
    Optional<SpotifyAccount> findBySystemUserIdAndAccountType(String systemUserId, AccountType accountType);
    void deleteBySystemUserIdAndAccountType(String systemUserId, AccountType accountType);
}
