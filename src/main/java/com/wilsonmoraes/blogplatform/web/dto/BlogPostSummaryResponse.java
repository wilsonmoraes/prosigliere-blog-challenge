package com.wilsonmoraes.blogplatform.web.dto;

import com.wilsonmoraes.blogplatform.repository.BlogPostSummaryProjection;

import java.time.Instant;

public record BlogPostSummaryResponse(
        Long id,
        String title,
        long commentCount,
        Instant createdAt
) {
    public static BlogPostSummaryResponse from(BlogPostSummaryProjection projection) {
        return new BlogPostSummaryResponse(
                projection.id(),
                projection.title(),
                projection.commentCount(),
                projection.createdAt()
        );
    }
}
