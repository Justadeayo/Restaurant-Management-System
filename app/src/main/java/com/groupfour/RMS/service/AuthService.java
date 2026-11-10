package com.groupfour.RMS.service;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;

public class AuthService {
    private UserDAO userDAO;
    private User currentUser;
    
    public AuthService() {
        this.userDAO = new UserDAO();
    }
    
    public boolean login(String username, String password) {
        currentUser = userDAO.authenticate(username, password);
        return currentUser != null;
    }
    
    public void logout() {
        currentUser = null;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public boolean isManager() {
        return currentUser != null && currentUser.isManager();
    }
    
    public boolean isCashier() {
        return currentUser != null && currentUser.isCashier();
    }
}
