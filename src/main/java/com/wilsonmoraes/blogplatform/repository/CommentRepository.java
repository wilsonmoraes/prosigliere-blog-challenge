package com.wilsonmoraes.blogplatform.repository;

import com.wilsonmoraes.blogplatform.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByBlogPost_Id(Long blogPostId, Pageable pageable);
}
