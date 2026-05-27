package com.wilsonmoraes.blogplatform.repository;

import com.wilsonmoraes.blogplatform.config.JpaAuditingConfig;
import com.wilsonmoraes.blogplatform.domain.BlogPost;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class BlogPostRepositoryTest {

    @Autowired
    private BlogPostRepository repository;

    @Test
    void findAllSummariesReturnsCommentCounts() {
        BlogPost first = new BlogPost("first", "c1");
        first.addComment("a", "hi");
        first.addComment("b", "yo");
        repository.save(first);

        BlogPost second = new BlogPost("second", "c2");
        repository.save(second);

        Page<BlogPostSummaryProjection> page = repository.findAllSummaries(
                PageRequest.of(0, 10, Sort.by("id"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(BlogPostSummaryProjection::title, BlogPostSummaryProjection::commentCount)
                .containsExactly(
                        Tuple.tuple("first", 2L),
                        Tuple.tuple("second", 0L)
                );
    }
}
