package com.wilsonmoraes.blogplatform.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBlogPostRequest(
        @NotBlank
        @Size(max = 200)
        @Schema(description = "Post title", example = "My first post")
        String title,

        @NotBlank
        @Size(max = 50_000)
        @Schema(description = "Post body in plain text or markdown", example = "Hello world!")
        String content
) {
}
