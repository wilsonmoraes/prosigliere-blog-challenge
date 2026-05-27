package com.wilsonmoraes.blogplatform.web;

import com.wilsonmoraes.blogplatform.domain.Comment;
import com.wilsonmoraes.blogplatform.service.BlogPostService;
import com.wilsonmoraes.blogplatform.web.dto.CommentResponse;
import com.wilsonmoraes.blogplatform.web.dto.CreateCommentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Manage comments on a blog post")
public class CommentController {

    private final BlogPostService blogPostService;

    @PostMapping
    @Operation(summary = "Add a comment to a blog post")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Comment comment = blogPostService.addComment(postId, request.author(), request.content());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/posts/{postId}/comments/{commentId}")
                .buildAndExpand(postId, comment.getId())
                .toUri();
        return ResponseEntity.created(location).body(CommentResponse.from(comment));
    }
}
