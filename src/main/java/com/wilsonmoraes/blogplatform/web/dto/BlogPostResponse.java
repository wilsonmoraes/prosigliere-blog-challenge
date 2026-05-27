package com.wilsonmoraes.blogplatform.web.dto;

import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;

import java.time.Instant;
import java.util.List;

public record BlogPostResponse(
        Long id,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments
) {
    public static BlogPostResponse of(BlogPost post, List<Comment> comments) {
        List<CommentResponse> mapped = comments.stream()
                .map(CommentResponse::from)
                .toList();
        return new BlogPostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                mapped
        );
    }
}
