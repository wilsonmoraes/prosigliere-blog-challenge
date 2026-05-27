package com.wilsonmoraes.blogplatform.repository;

import java.time.Instant;

public record BlogPostSummaryProjection(
        Long id,
        String title,
        long commentCount,
        Instant createdAt
) {
}
