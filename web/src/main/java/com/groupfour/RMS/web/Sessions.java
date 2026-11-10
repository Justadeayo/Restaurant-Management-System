package com.groupfour.RMS.web;

import com.groupfour.RMS.model.User;
import jakarta.servlet.http.HttpSession;

final class Sessions {
    private Sessions() {
    }

    static User manager(HttpSession session) {
        if (session.getAttribute("user") instanceof User user && user.getRole() == User.UserRole.MANAGER) {
            return user;
        }
        return null;
    }
}
