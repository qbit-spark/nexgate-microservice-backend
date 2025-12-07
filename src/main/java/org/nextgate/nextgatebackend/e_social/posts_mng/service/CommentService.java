package org.nextgate.nextgatebackend.e_social.posts_mng.service;

import org.nextgate.nextgatebackend.e_social.posts_mng.entity.PostCommentEntity;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.CreateCommentRequest;
import org.nextgate.nextgatebackend.e_social.posts_mng.payloads.UpdateCommentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {

    // Create and manage comments
    PostCommentEntity createComment(UUID postId, CreateCommentRequest request);
    PostCommentEntity updateComment(UUID commentId, UpdateCommentRequest request);
    void deleteComment(UUID commentId);

    // Get comments
    Page<PostCommentEntity> getComments(UUID postId, Pageable pageable);
    Page<PostCommentEntity> getReplies(UUID commentId, Pageable pageable);
    PostCommentEntity getCommentById(UUID commentId);

    // Pin/unpin comments (post author only)
    PostCommentEntity pinComment(UUID postId, UUID commentId);
    PostCommentEntity unpinComment(UUID commentId);

    // Like comments
    void likeComment(UUID commentId);
    void unlikeComment(UUID commentId);
}