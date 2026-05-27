package com.wilsonmoraes.blogplatform.web;

import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;
import com.wilsonmoraes.blogplatform.service.BlogPostService;
import com.wilsonmoraes.blogplatform.web.dto.BlogPostResponse;
import com.wilsonmoraes.blogplatform.web.dto.BlogPostSummaryResponse;
import com.wilsonmoraes.blogplatform.web.dto.CreateBlogPostRequest;
import com.wilsonmoraes.blogplatform.web.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Blog posts", description = "Manage blog posts")
public class BlogPostController {

    private static final int LATEST_COMMENTS_IN_DETAIL = 20;

    private final BlogPostService blogPostService;

    @GetMapping
    @Operation(summary = "List blog posts with their comment counts")
    public PagedResponse<BlogPostSummaryResponse> list(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PagedResponse.from(
                blogPostService.listSummaries(pageable),
                BlogPostSummaryResponse::from
        );
    }

    @PostMapping
    @Operation(summary = "Create a new blog post")
    public ResponseEntity<BlogPostResponse> create(@Valid @RequestBody CreateBlogPostRequest request) {
        BlogPost created = blogPostService.create(request.title(), request.content());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(BlogPostResponse.of(created, List.of()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a blog post by id, including its most recent comments")
    public BlogPostResponse get(@PathVariable Long id) {
        BlogPost post = blogPostService.findById(id);
        Pageable latest = PageRequest.of(0, LATEST_COMMENTS_IN_DETAIL, Sort.by(Sort.Direction.ASC, "createdAt"));
        List<Comment> comments = blogPostService.findCommentsByPostId(id, latest).getContent();
        return BlogPostResponse.of(post, comments);
    }
}
