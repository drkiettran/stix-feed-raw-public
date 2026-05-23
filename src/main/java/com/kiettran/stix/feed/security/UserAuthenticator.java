package com.kiettran.stix.feed.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory test-user authenticator. Production would consult an identity
 * provider; here we use a fixed set of in-memory credentials for demonstration.
 */
public final class UserAuthenticator {

    public record User(String username, String password, List<String> roles) {}

    private final Map<String, User> users = new HashMap<>();

    public UserAuthenticator add(User u) {
        users.put(u.username(), u);
        return this;
    }

    public Optional<User> authenticate(String username, String password) {
        User u = users.get(username);
        if (u == null) return Optional.empty();
        // Constant-time compare — in real systems passwords are hashed.
        if (!constantTimeEquals(u.password(), Objects.toString(password, ""))) {
            return Optional.empty();
        }
        return Optional.of(u);
    }

    public Set<String> knownUsernames() { return users.keySet(); }

    public static UserAuthenticator withTestUsers() {
        return new UserAuthenticator()
            .add(new User("analyst", "analyst-pass", List.of("analyst", "reader")))
            .add(new User("reader",  "reader-pass",  List.of("reader")))
            .add(new User("admin",   "admin-pass",   List.of("analyst", "reader")));
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return a == b;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= a.charAt(i) ^ b.charAt(i);
        return diff == 0;
    }
}
