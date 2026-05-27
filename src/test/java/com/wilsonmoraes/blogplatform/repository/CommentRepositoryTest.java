package com.wilsonmoraes.blogplatform.repository;

import com.wilsonmoraes.blogplatform.config.JpaAuditingConfig;
import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;
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
class CommentRepositoryTest {

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void findByBlogPostIdReturnsOnlyMatchingComments() {
        BlogPost first = blogPostRepository.save(new BlogPost("first", "c1"));
        BlogPost second = blogPostRepository.save(new BlogPost("second", "c2"));

        commentRepository.save(new Comment(first, "alice", "hi"));
        commentRepository.save(new Comment(first, "bob", "yo"));
        commentRepository.save(new Comment(second, "carol", "elsewhere"));

        Page<Comment> page = commentRepository.findByBlogPost_Id(
                first.getId(), PageRequest.of(0, 10, Sort.by("createdAt"))
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Comment::getAuthor)
                .containsExactlyInAnyOrder("alice", "bob");
        assertThat(page.getContent().get(0).getId()).isNotNull();
        assertThat(page.getContent().get(0).getCreatedAt()).isNotNull();
    }
}
