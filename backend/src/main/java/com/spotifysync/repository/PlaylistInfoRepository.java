package com.spotifysync.repository;

import com.spotifysync.entity.PlaylistInfo;
import com.spotifysync.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistInfoRepository extends JpaRepository<PlaylistInfo, Long> {
    List<PlaylistInfo> findByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
    void deleteByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
}
