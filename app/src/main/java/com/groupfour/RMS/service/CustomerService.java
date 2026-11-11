package com.groupfour.RMS.service;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;

// Customer accounts. Every screen signs customers up through here, so the rules are the same everywhere.
public class CustomerService {
    private final UserDAO userDAO = new UserDAO();

    public void signUp(String username, String password, String confirmPassword) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty() || password == null || password.length() < 6) {
            throw new IllegalArgumentException("Choose a username and a password of at least 6 characters.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("The two passwords do not match.");
        }
        if (!userDAO.createUser(name, password, User.UserRole.CUSTOMER)) {
            throw new IllegalArgumentException("That username is already taken.");
        }
    }
}
