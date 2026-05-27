package com.wilsonmoraes.blogplatform.repository;

import com.wilsonmoraes.blogplatform.domain.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {

    @Query("""
            select new com.wilsonmoraes.blogplatform.repository.BlogPostSummaryProjection(
                p.id,
                p.title,
                (select count(c) from Comment c where c.blogPost.id = p.id),
                p.createdAt
            )
            from BlogPost p
            """)
    Page<BlogPostSummaryProjection> findAllSummaries(Pageable pageable);
}
