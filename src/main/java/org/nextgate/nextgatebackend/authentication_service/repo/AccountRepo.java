package org.nextgate.nextgatebackend.authentication_service.repo;


import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepo extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findAccountEntitiesByEmailOrPhoneNumberOrUserName(String email, String phoneNumber, String userName);
    Optional<AccountEntity> findAccountEntitiesByUserName(String userName);
    Optional<AccountEntity> findByEmail(String email);
    Optional<AccountEntity> findByUserName(String username);
    Boolean existsByPhoneNumberOrEmailOrUserName(String phoneNumber, String email, String userName);

    Optional<AccountEntity> findByEmailOrPhoneNumberOrUserName(String email, String phoneNumber, String userName);
    Boolean existsByUserName(String userName);

}
