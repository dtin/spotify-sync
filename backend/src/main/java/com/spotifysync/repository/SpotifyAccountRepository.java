package com.spotifysync.repository;

import com.spotifysync.entity.SpotifyAccount;
import com.spotifysync.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotifyAccountRepository extends JpaRepository<SpotifyAccount, Long> {
    Optional<SpotifyAccount> findByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
    void deleteByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
}
