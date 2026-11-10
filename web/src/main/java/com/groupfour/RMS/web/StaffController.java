package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StaffController {
    private final UserDAO userDAO = new UserDAO();

    @GetMapping("/manager/staff")
    String staff(HttpSession session, Model model) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("users", userDAO.getAllUsers());
        model.addAttribute("roles", new User.UserRole[]{User.UserRole.CASHIER, User.UserRole.MANAGER});
        return "staff";
    }

    @PostMapping("/manager/staff")
    String addUser(@RequestParam String username,
                   @RequestParam String password,
                   @RequestParam User.UserRole role,
                   HttpSession session,
                   RedirectAttributes redirect) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        if (username == null || username.isBlank()) {
            redirect.addFlashAttribute("error", "Username is required.");
        } else if (password == null || password.length() < 6) {
            redirect.addFlashAttribute("error", "Password must be at least 6 characters.");
        } else if (role == null) {
            redirect.addFlashAttribute("error", "Choose a role.");
        } else if (userDAO.createUser(username.trim(), password, role)) {
            redirect.addFlashAttribute("message", "Created " + username.trim() + " as " + role + ".");
        } else {
            redirect.addFlashAttribute("error", "Could not create the account. The username may already exist.");
        }
        return "redirect:/manager/staff";
    }

    @PostMapping("/manager/staff/{id}/status")
    String setStatus(@PathVariable int id,
                     @RequestParam boolean active,
                     HttpSession session,
                     RedirectAttributes redirect) {
        User manager = Sessions.manager(session);
        if (manager == null) {
            return "redirect:/login";
        }
        if (manager.getId() == id && !active) {
            redirect.addFlashAttribute("error", "You cannot deactivate your own account.");
        } else if (userDAO.updateUserStatus(id, active)) {
            redirect.addFlashAttribute("message", active ? "Account activated." : "Account deactivated.");
        } else {
            redirect.addFlashAttribute("error", "The account could not be updated.");
        }
        return "redirect:/manager/staff";
    }
}
