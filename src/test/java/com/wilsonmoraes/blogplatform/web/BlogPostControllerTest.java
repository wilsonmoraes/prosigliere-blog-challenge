package com.wilsonmoraes.blogplatform.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wilsonmoraes.blogplatform.domain.BlogPost;
import com.wilsonmoraes.blogplatform.domain.Comment;
import com.wilsonmoraes.blogplatform.repository.BlogPostSummaryProjection;
import com.wilsonmoraes.blogplatform.service.BlogPostService;
import com.wilsonmoraes.blogplatform.web.dto.CreateBlogPostRequest;
import com.wilsonmoraes.blogplatform.web.dto.CreateCommentRequest;
import com.wilsonmoraes.blogplatform.web.exception.BlogPostNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BlogPostController.class, CommentController.class})
class BlogPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BlogPostService blogPostService;

    @Test
    void listReturnsPagedSummaries() throws Exception {
        BlogPostSummaryProjection projection = new BlogPostSummaryProjection(
                1L, "Hello", 3L, Instant.parse("2026-01-01T10:00:00Z")
        );
        when(blogPostService.listSummaries(any()))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 20), 1));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Hello"))
                .andExpect(jsonPath("$.content[0].commentCount").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        BlogPost created = postWithId(7L, "title", "content");
        when(blogPostService.create(eq("title"), eq("content"))).thenReturn(created);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlogPostRequest("title", "content"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/posts/7")))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(0));
    }

    @Test
    void createReturns400OnInvalidPayload() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBlogPostRequest("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getReturnsPostWithComments() throws Exception {
        BlogPost post = postWithId(2L, "T", "C");
        Comment comment = new Comment(post, "alice", "hi");
        setId(comment, 100L);
        when(blogPostService.findById(2L)).thenReturn(post);
        when(blogPostService.findCommentsByPostId(eq(2L), any()))
                .thenReturn(new PageImpl<>(List.of(comment)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.comments[0].id").value(100))
                .andExpect(jsonPath("$.comments[0].author").value("alice"));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(blogPostService.findById(99L)).thenThrow(new BlogPostNotFoundException(99L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("99")));
    }

    @Test
    void addCommentReturns201() throws Exception {
        BlogPost post = postWithId(5L, "T", "C");
        Comment comment = new Comment(post, "bob", "nice");
        setId(comment, 10L);
        when(blogPostService.addComment(eq(5L), eq("bob"), eq("nice"))).thenReturn(comment);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/posts/5/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("bob", "nice"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.author").value("bob"));
    }

    @Test
    void listCommentsReturnsPaged() throws Exception {
        BlogPost post = postWithId(5L, "T", "C");
        Comment c1 = new Comment(post, "alice", "first");
        setId(c1, 1L);
        Comment c2 = new Comment(post, "bob", "second");
        setId(c2, 2L);
        when(blogPostService.findCommentsByPostId(eq(5L), any()))
                .thenReturn(new PageImpl<>(List.of(c1, c2), PageRequest.of(0, 20), 2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/posts/5/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].author").value("alice"))
                .andExpect(jsonPath("$.content[1].author").value("bob"));
    }

    private BlogPost postWithId(Long id, String title, String content) {
        BlogPost post = new BlogPost(title, content);
        setId(post, id);
        return post;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
