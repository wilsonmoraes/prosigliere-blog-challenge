package com.wilsonmoraes.blogplatform.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "Comment author display name", example = "Jane Doe")
        String author,

        @NotBlank
        @Size(max = 5_000)
        @Schema(description = "Comment body", example = "Great post!")
        String content
) {
}
