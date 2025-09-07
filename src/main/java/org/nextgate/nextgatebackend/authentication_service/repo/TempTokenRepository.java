package org.nextgate.nextgatebackend.authentication_service.repo;


import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.entity.TempTokenEntity;
import org.nextgate.nextgatebackend.authentication_service.enums.TempTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TempTokenRepository extends JpaRepository<TempTokenEntity, UUID> {


    Optional<TempTokenEntity> findByTokenHash(String tokenHash);


    List<TempTokenEntity> findByAccountAndPurpose(AccountEntity account, TempTokenPurpose purpose);


    List<TempTokenEntity> findByUserIdentifierAndPurpose(String userIdentifier, TempTokenPurpose purpose);


    List<TempTokenEntity> findByAccountAndPurposeAndIsUsed(AccountEntity account, TempTokenPurpose purpose, Boolean isUsed);


    List<TempTokenEntity> findByUserIdentifierAndPurposeAndIsUsed(String userIdentifier, TempTokenPurpose purpose, Boolean isUsed);


    List<TempTokenEntity> findByAccountAndPurposeAndIsUsedAndExpiresAtAfter(
            AccountEntity account,
            TempTokenPurpose purpose,
            Boolean isUsed,
            LocalDateTime expiresAt
    );


    List<TempTokenEntity> findByUserIdentifierAndPurposeAndIsUsedAndExpiresAtAfter(
            String userIdentifier,
            TempTokenPurpose purpose,
            Boolean isUsed,
            LocalDateTime expiresAt
    );


    List<TempTokenEntity> findByAccountAndPurposeAndCreatedAtAfter(
            AccountEntity account,
            TempTokenPurpose purpose,
            LocalDateTime createdAt
    );


    List<TempTokenEntity> findByUserIdentifierAndPurposeAndCreatedAtAfter(
            String userIdentifier,
            TempTokenPurpose purpose,
            LocalDateTime createdAt
    );


    List<TempTokenEntity> findByExpiresAtBefore(LocalDateTime expiresAt);


    List<TempTokenEntity> findByIsUsedAndCreatedAtBefore(Boolean isUsed, LocalDateTime createdAt);
}