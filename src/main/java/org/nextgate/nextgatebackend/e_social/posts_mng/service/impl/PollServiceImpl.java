package org.nextgate.nextgatebackend.e_social.posts_mng.service.impl;

import lombok.RequiredArgsConstructor;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollOptionEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PollVoteEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.enums.PostType;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.PollResultsResponse;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PollOptionRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PollRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PollVoteRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.repo.PostRepository;
import org.nextgate.nextgatebackend.e_social.posts_mng.service.PollService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollServiceImpl implements PollService {

    private final PostRepository postRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final AccountRepo accountRepo;

    @Override
    @Transactional
    public void voteOnPoll(UUID postId, List<UUID> optionIds) {

        AccountEntity voter = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getPostType() != PostType.POLL) {
            throw new IllegalArgumentException("This post is not a poll");
        }

        PollEntity poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            throw new IllegalArgumentException("This poll has expired");
        }

        boolean hasVoted = pollVoteRepository.existsByPollIdAndVoterId(poll.getId(), voter.getId());
        if (hasVoted && !poll.isAllowMultipleVotes()) {
            throw new IllegalArgumentException("You have already voted on this poll");
        }

        if (optionIds.size() > 1 && !poll.isAllowMultipleVotes()) {
            throw new IllegalArgumentException("This poll only allows voting for one option");
        }

        for (UUID optionId : optionIds) {
            PollOptionEntity option = pollOptionRepository.findById(optionId)
                    .orElseThrow(() -> new IllegalArgumentException("Poll option not found: " + optionId));

            if (!option.getPollId().equals(poll.getId())) {
                throw new IllegalArgumentException("Option does not belong to this poll");
            }

            if (pollVoteRepository.existsByOptionIdAndVoterId(optionId, voter.getId())) {
                continue;
            }

            PollVoteEntity vote = new PollVoteEntity();
            vote.setPollId(poll.getId());
            vote.setOptionId(optionId);
            vote.setVoterId(voter.getId());
            pollVoteRepository.save(vote);

            option.setVotesCount(option.getVotesCount() + 1);
            pollOptionRepository.save(option);
        }

        poll.setTotalVotes(poll.getTotalVotes() + 1);
        pollRepository.save(poll);
    }

    @Override
    @Transactional
    public void removeVote(UUID postId) {
        AccountEntity voter = getAuthenticatedAccount();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getPostType() != PostType.POLL) {
            throw new IllegalArgumentException("This post is not a poll");
        }

        PollEntity poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        if (poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt())) {
            throw new IllegalArgumentException("Cannot remove vote from expired poll");
        }

        List<PollVoteEntity> votes = pollVoteRepository.findByPollIdAndVoterId(poll.getId(), voter.getId());

        if (votes.isEmpty()) {
            throw new IllegalArgumentException("You have not voted on this poll");
        }

        for (PollVoteEntity vote : votes) {
            PollOptionEntity option = pollOptionRepository.findById(vote.getOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("Poll option not found"));

            option.setVotesCount(Math.max(0, option.getVotesCount() - 1));
            pollOptionRepository.save(option);

            pollVoteRepository.delete(vote);
        }

        poll.setTotalVotes(Math.max(0, poll.getTotalVotes() - 1));
        pollRepository.save(poll);
    }

    @Override
    @Transactional(readOnly = true)
    public PollResultsResponse getPollResults(UUID postId) {
        AccountEntity currentUser = getAuthenticatedAccountOrNull();

        PostEntity post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        if (post.getPostType() != PostType.POLL) {
            throw new IllegalArgumentException("This post is not a poll");
        }

        PollEntity poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        List<PollOptionEntity> options = pollOptionRepository.findByPollIdOrderByOptionOrder(poll.getId());

        PollResultsResponse response = new PollResultsResponse();
        response.setPollId(poll.getId());
        response.setTitle(poll.getTitle());
        response.setDescription(poll.getDescription());
        response.setTotalVotes(poll.getTotalVotes());
        response.setAllowMultipleVotes(poll.isAllowMultipleVotes());
        response.setAnonymous(poll.isAnonymous());
        response.setExpiresAt(poll.getExpiresAt());
        response.setHasExpired(poll.getExpiresAt() != null && LocalDateTime.now().isAfter(poll.getExpiresAt()));

        if (currentUser != null) {
            response.setUserHasVoted(pollVoteRepository.existsByPollIdAndVoterId(poll.getId(), currentUser.getId()));
        } else {
            response.setUserHasVoted(false);
        }

        List<UUID> userVotedOptions = currentUser != null
                ? getUserVotedOptions(postId, currentUser.getId())
                : List.of();

        List<PollResultsResponse.PollOptionResult> optionResults = options.stream()
                .map(option -> {
                    PollResultsResponse.PollOptionResult result = new PollResultsResponse.PollOptionResult();
                    result.setOptionId(option.getId());
                    result.setOptionText(option.getOptionText());
                    result.setOptionImageUrl(option.getOptionImageUrl());
                    result.setOptionOrder(option.getOptionOrder());
                    result.setVotesCount(option.getVotesCount());

                    double percentage = poll.getTotalVotes() > 0
                            ? (option.getVotesCount() * 100.0) / poll.getTotalVotes()
                            : 0.0;
                    result.setPercentage(Math.round(percentage * 10.0) / 10.0);

                    result.setUserVoted(userVotedOptions.contains(option.getId()));

                    return result;
                })
                .collect(Collectors.toList());

        response.setOptions(optionResults);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserVoted(UUID postId, UUID userId) {
        PollEntity poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        return pollVoteRepository.existsByPollIdAndVoterId(poll.getId(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getUserVotedOptions(UUID postId, UUID userId) {
        PollEntity poll = pollRepository.findByPostId(postId)
                .orElseThrow(() -> new IllegalArgumentException("Poll not found"));

        return pollVoteRepository.findByPollIdAndVoterId(poll.getId(), userId)
                .stream()
                .map(PollVoteEntity::getOptionId)
                .collect(Collectors.toList());
    }

    private AccountEntity getAuthenticatedAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }
        UUID userId = UUID.fromString(authentication.getName());
        return accountRepo.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    private AccountEntity getAuthenticatedAccountOrNull() {
        try {
            return getAuthenticatedAccount();
        } catch (Exception e) {
            return null;
        }
    }
}