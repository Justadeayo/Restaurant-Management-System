package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;
import jakarta.servlet.http.HttpSession;

final class Sessions {
    private Sessions() {
    }

    // Returns the signed-in user only while the account is still active.
    // A deactivated or removed account is signed out on its next request.
    static User signedIn(HttpSession session) {
        if (!(session.getAttribute("user") instanceof User user)) {
            return null;
        }
        if (!new UserDAO().isActive(user.getId())) {
            session.removeAttribute("user");
            return null;
        }
        return user;
    }

    static User manager(HttpSession session) {
        User user = signedIn(session);
        return user != null && user.getRole() == User.UserRole.MANAGER ? user : null;
    }
}
