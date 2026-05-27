package com.wilsonmoraes.blogplatform.service;

import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;
import com.wilsonmoraes.blogplatform.repository.BlogPostRepository;
import com.wilsonmoraes.blogplatform.repository.BlogPostSummaryProjection;
import com.wilsonmoraes.blogplatform.web.exception.BlogPostNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BlogPostService {

    private final BlogPostRepository blogPostRepository;

    @Transactional(readOnly = true)
    public Page<BlogPostSummaryProjection> listSummaries(Pageable pageable) {
        return blogPostRepository.findAllSummaries(pageable);
    }

    @Transactional(readOnly = true)
    public BlogPost findById(Long id) {
        return blogPostRepository.findById(id)
                .orElseThrow(() -> new BlogPostNotFoundException(id));
    }

    public BlogPost create(String title, String content) {
        return blogPostRepository.save(new BlogPost(title, content));
    }

    public Comment addComment(Long postId, String author, String content) {
        BlogPost post = blogPostRepository.findById(postId)
                .orElseThrow(() -> new BlogPostNotFoundException(postId));
        Comment comment = post.addComment(author, content);
        blogPostRepository.save(post);
        return comment;
    }
}
