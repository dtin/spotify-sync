package com.spotifysync.repository;

import com.spotifysync.entity.AlbumInfo;
import com.spotifysync.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlbumInfoRepository extends JpaRepository<AlbumInfo, Long> {
    List<AlbumInfo> findByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
    void deleteByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
}
