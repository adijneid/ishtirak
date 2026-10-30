package org.example.ishtirak.client;

import org.example.ishtirak.common.dto.LoginResponse;

import java.time.YearMonth;

/** Who is logged in (token, role, name) and the billing month every view reuses. */
public final class SessionContext {
    private static String token;
    private static String role;
    private static String fullName;
    private static String username;
    private static YearMonth selectedMonth = YearMonth.now();

    private SessionContext() {
    }

    public static void start(LoginResponse login) {
        token = login.token();
        role = login.role();
        fullName = login.fullName();
        username = login.username();
    }

    public static void clear() {
        token = null;
        role = null;
        fullName = null;
        username = null;
    }

    public static String getToken() {
        return token;
    }

    public static String getRole() {
        return role;
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role);
    }

    public static String getFullName() {
        return fullName;
    }

    public static String getUsername() {
        return username;
    }

    public static YearMonth getSelectedMonth() {
        return selectedMonth;
    }

    public static void setSelectedMonth(YearMonth month) {
        if (month != null) {
            selectedMonth = month;
        }
    }
}
