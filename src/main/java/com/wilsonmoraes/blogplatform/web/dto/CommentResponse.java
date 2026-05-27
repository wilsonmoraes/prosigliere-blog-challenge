package com.wilsonmoraes.blogplatform.web.dto;

import com.wilsonmoraes.blogplatform.domain.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String author,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
