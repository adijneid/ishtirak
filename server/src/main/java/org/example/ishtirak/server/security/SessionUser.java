package org.example.ishtirak.server.security;

import org.example.ishtirak.common.model.Role;

/** The authenticated caller of the current request. */
public record SessionUser(Integer id, String username, Role role) {
}
