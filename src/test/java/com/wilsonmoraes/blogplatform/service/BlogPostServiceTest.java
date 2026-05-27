package com.wilsonmoraes.blogplatform.service;

import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;
import com.wilsonmoraes.blogplatform.repository.BlogPostRepository;
import com.wilsonmoraes.blogplatform.repository.CommentRepository;
import com.wilsonmoraes.blogplatform.web.exception.BlogPostNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogPostServiceTest {

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private BlogPostService service;

    @Test
    void createPersistsBlogPostWithProvidedTitleAndContent() {
        when(blogPostRepository.save(any(BlogPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BlogPost created = service.create("title", "content");

        assertThat(created.getTitle()).isEqualTo("title");
        assertThat(created.getContent()).isEqualTo("content");
        verify(blogPostRepository).save(any(BlogPost.class));
    }

    @Test
    void findByIdReturnsPostWhenExists() {
        BlogPost post = new BlogPost("t", "c");
        when(blogPostRepository.findById(1L)).thenReturn(Optional.of(post));

        BlogPost found = service.findById(1L);

        assertThat(found).isSameAs(post);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(blogPostRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BlogPostNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void addCommentPersistsCommentWhenPostExists() {
        BlogPost reference = new BlogPost("t", "c");
        when(blogPostRepository.existsById(1L)).thenReturn(true);
        when(blogPostRepository.getReferenceById(1L)).thenReturn(reference);
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Comment comment = service.addComment(1L, "alice", "nice");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getBlogPost()).isSameAs(reference);
        assertThat(comment.getAuthor()).isEqualTo("alice");
        assertThat(comment.getContent()).isEqualTo("nice");
    }

    @Test
    void addCommentThrowsWhenPostMissing() {
        when(blogPostRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> service.addComment(42L, "a", "b"))
                .isInstanceOf(BlogPostNotFoundException.class);
    }

    @Test
    void findCommentsByPostIdThrowsWhenPostMissing() {
        when(blogPostRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.findCommentsByPostId(7L, org.springframework.data.domain.PageRequest.of(0, 10)))
                .isInstanceOf(BlogPostNotFoundException.class);
    }
}
