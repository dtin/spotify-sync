package com.spotifysync.repository;

import com.spotifysync.entity.PlaylistInfo;
import com.spotifysync.enums.AccountType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PlaylistInfoRepository extends MongoRepository<PlaylistInfo, String> {
    List<PlaylistInfo> findByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
    void deleteByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
}
