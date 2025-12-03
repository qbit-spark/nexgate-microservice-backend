package org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.user_relationships.follow_system.repo.FollowRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.BlockEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.entity.MuteEntity;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo.BlockRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.repo.MuteRepository;
import org.nextgate.nextgatebackend.e_social.user_relationships.privacy_controls.service.PrivacyControlService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrivacyControlServiceImpl implements PrivacyControlService {

    private final BlockRepository blockRepository;
    private final MuteRepository muteRepository;
    private final FollowRepository followRepository;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public BlockEntity blockUser(UUID userId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID blockerId = authenticatedUser.getId();

        if (blockerId.equals(userId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, userId)) {
            throw new IllegalStateException("User already blocked");
        }

        followRepository.deleteByFollowerIdAndFollowingId(blockerId, userId);
        followRepository.deleteByFollowerIdAndFollowingId(userId, blockerId);

        BlockEntity block = new BlockEntity();
        block.setBlockerId(blockerId);
        block.setBlockedId(userId);
        block.setCreatedAt(LocalDateTime.now());

        return blockRepository.save(block);
    }

    @Override
    @Transactional
    public void unblockUser(UUID userId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID blockerId = authenticatedUser.getId();

        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, userId);
    }

    @Override
    @Transactional
    public MuteEntity muteUser(UUID userId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID muterId = authenticatedUser.getId();

        if (muterId.equals(userId)) {
            throw new IllegalArgumentException("Cannot mute yourself");
        }

        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (muteRepository.existsByMuterIdAndMutedId(muterId, userId)) {
            throw new IllegalStateException("User already muted");
        }

        MuteEntity mute = new MuteEntity();
        mute.setMuterId(muterId);
        mute.setMutedId(userId);
        mute.setCreatedAt(LocalDateTime.now());

        return muteRepository.save(mute);
    }

    @Override
    @Transactional
    public void unmuteUser(UUID userId) {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        UUID muterId = authenticatedUser.getId();

        accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        muteRepository.deleteByMuterIdAndMutedId(muterId, userId);
    }

    @Override
    public List<BlockEntity> getBlockedUsers() {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        return blockRepository.findByBlockerIdOrderByCreatedAtDesc(authenticatedUser.getId());
    }

    @Override
    public List<MuteEntity> getMutedUsers() {
        AccountEntity authenticatedUser = getAuthenticatedAccount();
        return muteRepository.findByMuterIdOrderByCreatedAtDesc(authenticatedUser.getId());
    }

    @Override
    public boolean isBlocked(UUID blockerId, UUID blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public boolean isMuted(UUID muterId, UUID mutedId) {
        return muteRepository.existsByMuterIdAndMutedId(muterId, mutedId);
    }

    private AccountEntity getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String userName = userDetails.getUsername();
            return accountRepo.findByUserName(userName)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        }
        throw new IllegalArgumentException("User not authenticated");
    }
}