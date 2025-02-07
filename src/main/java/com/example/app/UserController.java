package com.example.app;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final List<User> users = new ArrayList<>();
    private User loggedInUser = null;

    public UserController() {
        users.add(new User("admin", hashPassword("pasadmin"), User.Role.ADMIN));
        users.add(new User("user", hashPassword("pasuser"), User.Role.USER));
    }

    public String hashPassword(String password) {
        try {
            // algorytm haszowania
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // hasło w bajty i heszujemy
            byte[] hash = md.digest(password.getBytes());
            // kodujemy hasz w stroku korzystając z Base64
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            logger.warn("Error while hashing the password: ", e);
            throw new RuntimeException("Error while hashing the password", e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User loginUser, HttpServletResponse response) {
        for (User user : users) {
            if (user.getUsername().equals(loginUser.getUsername()) &&
                user.getPassword().equals(hashPassword(loginUser.getPassword()))) {

                loggedInUser = user;

                Cookie cookie = new Cookie("sessionToken", user.getUsername());
                cookie.setHttpOnly(true);
                cookie.setMaxAge(7 * 24 * 60 * 60);
                cookie.setSecure(false);
                cookie.setPath("/");

                response.addHeader("Set-Cookie",
                        "sessionToken=" + user.getUsername() +
                                "; HttpOnly; Max-Age=" + (7 * 24 * 60 * 60) +
                                "; Path=/; SameSite=None; Secure=false");

                response.addCookie(cookie);
                logger.info("Login successful for user: {}", loginUser.getUsername());

                Map<String, String> responseBody = new HashMap<>();
                responseBody.put("message", "Login successful");
                responseBody.put("role", user.getRole().name());

                return ResponseEntity.ok(responseBody);
            }
        }
        logger.warn("Login failed for user: {}", loginUser.getUsername());

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("message", "Login failed: Invalid username or password.");
        responseBody.put("role", "");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@CookieValue(value = "sessionToken", defaultValue = "") String sessionToken, HttpServletResponse response) {
        if (sessionToken.isEmpty()) {
            logger.warn("Logout attempt with no session token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No user is currently logged in.");
        }

        for (User user : users) {
            if (user.getUsername().equals(sessionToken)) {
                loggedInUser = null;
                Cookie cookie = new Cookie("sessionToken", "");
                cookie.setMaxAge(0);
                cookie.setPath("/");
                response.addCookie(cookie);

                logger.info("Logging out user: {}", user.getUsername());
                return ResponseEntity.ok("User " + user.getUsername() + " logged out successfully.");
            }
        }

        logger.warn("Logout failed due to invalid session token: {}", sessionToken);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session token.");
    }

    @GetMapping("/current")
    public ResponseEntity<User> getCurrentUser(@CookieValue(value = "sessionToken", defaultValue = "") String sessionToken) {
        if (!sessionToken.isEmpty()) {
            for (User user : users) {
                if (user.getUsername().equals(sessionToken)) {
                    logger.info("Current logged-in user from sessionToken: {}", user.getUsername());
                    return ResponseEntity.ok(user);
                }
            }
        }

        logger.warn("No user is currently logged in.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    @PostMapping("/create")
    public String createUser(@RequestBody User newUser) {
        for (User user : users) {
            if (user.getUsername().equals(newUser.getUsername())) {
                logger.warn("User creation failed: Username {} already exists.", newUser.getUsername());
                return "User creation failed: Username already exists.";
            }
        }

        String hashedPassword = hashPassword(newUser.getPassword());
        newUser.setPassword(hashedPassword);

        newUser.setRole(User.Role.USER);

        users.add(newUser);
        logger.info("User {} created successfully.", newUser.getUsername());
        return "User " + newUser.getUsername() + " created successfully.";
    }

    @DeleteMapping("/delete")
    public String deleteUser() {
        if (loggedInUser != null) {
            users.removeIf(user -> user.getUsername().equals(loggedInUser.getUsername()));
            String username = loggedInUser.getUsername();
            loggedInUser = null;
            logger.info("User {} deleted successfully.", username);
            return "User " + username + " deleted successfully.";
        }
        logger.warn("Delete attempt with no user logged in.");
        return "No user is currently logged in.";
    }

    @GetMapping
    public List<String> getAllUsersWithHashedPasswords() {
        List<String> result = new ArrayList<>();
        for (User user : users) {
            result.add("Username: " + user.getUsername() + ", Password (hashed): " + user.getPassword());
        }
        return result;
    }
}
