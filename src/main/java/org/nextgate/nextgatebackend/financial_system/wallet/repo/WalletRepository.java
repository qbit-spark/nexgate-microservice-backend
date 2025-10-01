package org.nextgate.nextgatebackend.financial_system.wallet.repo;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.financial_system.wallet.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    // Find wallet by user account
    Optional<WalletEntity> findByAccount(AccountEntity account);


    // Check if wallet exists for account
    boolean existsByAccount(AccountEntity account);

    // Find all active wallets
    List<WalletEntity> findByIsActiveTrue();

    // Find all inactive wallets
    List<WalletEntity> findByIsActiveFalse();

    // Find wallets by active status
    List<WalletEntity> findByIsActive(Boolean isActive);

    // Find wallets created after a date
    List<WalletEntity> findByCreatedAtAfter(LocalDateTime date);

    // Find wallets deactivated by a specific admin
    List<WalletEntity> findByDeactivatedBy(UUID adminId);

    // Find wallets with no recent activity
    List<WalletEntity> findByLastActivityAtBeforeAndIsActiveTrue(LocalDateTime date);

    // Count active wallets
    long countByIsActiveTrue();

    // Count inactive wallets
    long countByIsActiveFalse();
}