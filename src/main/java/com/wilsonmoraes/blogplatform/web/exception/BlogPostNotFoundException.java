package com.wilsonmoraes.blogplatform.web.exception;

public class BlogPostNotFoundException extends RuntimeException {
    public BlogPostNotFoundException(Long id) {
        super("Blog post with id %d was not found".formatted(id));
    }
}
