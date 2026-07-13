package com.spotifysync.repository;

import com.spotifysync.entity.AlbumInfo;
import com.spotifysync.enums.AccountType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AlbumInfoRepository extends MongoRepository<AlbumInfo, String> {
    List<AlbumInfo> findByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
    void deleteByUserSessionIdAndAccountType(String userSessionId, AccountType accountType);
}
