package com.groupfour.RMS.web;

import com.groupfour.RMS.model.User;
import com.groupfour.RMS.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class KitchenController {
    private final OrderService orderService = new OrderService();

    private User kitchenUser(HttpSession session) {
        User user = Sessions.signedIn(session);
        return user != null && user.getRole() == User.UserRole.KITCHEN ? user : null;
    }

    @GetMapping("/kitchen")
    String queue(HttpSession session, Model model) {
        if (kitchenUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderService.getKitchenQueue());
        return "kitchen";
    }

    @PostMapping("/kitchen/orders/{id}/ready")
    String ready(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        User user = kitchenUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            orderService.advanceStatus(id, user);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/kitchen";
    }
}
