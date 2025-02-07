package com.example.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger logger = LoggerFactory.getLogger(PostController.class);

    private final List<Post> posts = new ArrayList<>();
    private int nextId = 1;

    private final UserController userController;

    public PostController(UserController userController) {
        this.userController = userController;
    }

    @PostMapping
    public Post createPost(@RequestBody String content, @CookieValue(value = "sessionToken", defaultValue = "") String sessionToken) {

        ResponseEntity<User> response = userController.getCurrentUser(sessionToken);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            logger.warn("User not logged in, cannot create post.");
            throw new RuntimeException("User not logged in, cannot create post.");
        }

        User currentUser = response.getBody();

        if (currentUser == null) {
            logger.warn("No current user found.");
            throw new RuntimeException("No current user found.");
        }

        Post post = new Post(nextId, content, currentUser.getUsername());
        nextId += 1;
        posts.add(post);
        logger.info("Post created by user: {} with content: {}", currentUser.getUsername(), content);
        return post;
    }

    @GetMapping
    public List<Post> getAllPosts() {
        logger.info("Getting all posts");
        return posts;
    }

    @PutMapping("/{id}")
    public Post updatePost(@PathVariable int id, @RequestBody String content, @CookieValue(value = "sessionToken", defaultValue = "") String sessionToken) {
        ResponseEntity<User> response = userController.getCurrentUser(sessionToken);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            logger.warn("Unauthorized attempt to update post with id: {}", id);
            throw new RuntimeException("User not logged in, cannot update post.");
        }

        User currentUser = response.getBody();
        if (currentUser == null) {
            logger.warn("No current user found for updating post with id: {}", id);
            throw new RuntimeException("No current user found.");
        }

        for (Post post : posts) {
            if (post.getId().equals(id) && (post.getAuthor().equals(currentUser.getUsername()) ||
                currentUser.getRole() == User.Role.ADMIN)) {
                post.setContent(content);
                logger.info("Post with id: {} updated by user: {}", id, currentUser.getUsername());
                return post;
            }
        }

        logger.error("Post not found or not owned by user for editing: {}", currentUser.getUsername());
        throw new RuntimeException("ERROR: Post not found or not owned by you!");
    }

    @DeleteMapping("/{id}")
    public Post deletePost(@PathVariable int id, @CookieValue(value = "sessionToken", defaultValue = "") String sessionToken) {
        ResponseEntity<User> response = userController.getCurrentUser(sessionToken);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            logger.warn("Unauthorized attempt to delete post with id: {}", id);
            throw new RuntimeException("User not logged in, cannot delete post.");
        }

        User currentUser = response.getBody();
        if (currentUser == null) {
            logger.warn("No current user found for deleting post with id: {}", id);
            throw new RuntimeException("No current user found.");
        }

        for (Post post : posts) {
            if (post.getId().equals(id) && (post.getAuthor().equals(currentUser.getUsername()) ||
                currentUser.getRole() == User.Role.ADMIN)) {
                logger.info("Post with id: {} deleted by user: {}", id, currentUser.getUsername());
                posts.remove(post);
                return post;
            }
        }
        logger.error("Post not found or not owned by user for deleting: {}", currentUser.getUsername());
        throw new RuntimeException("ERROR: Post not found or not owned by you!");
    }

    @GetMapping("/search")
    public List<Post> searchPosts(@RequestParam String example) {
        if (example == null || example.isEmpty()) {
            logger.error("Search example parameter is missing or empty");
            throw new RuntimeException("ERROR: Search example parameter is missing or empty!");
        }

        List<Post> suitablePosts = new ArrayList<>();
        for (Post post : posts) {
            if (post.getContent().toLowerCase().contains(example.toLowerCase())) {
                suitablePosts.add(post);
            }
        }

        logger.info("Found {} posts matching the search example: {}", suitablePosts.size(), example);
        return suitablePosts;
    }

}
