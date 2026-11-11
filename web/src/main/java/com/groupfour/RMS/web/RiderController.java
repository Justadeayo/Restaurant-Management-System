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
public class RiderController {
    private final OrderService orderService = new OrderService();

    private User riderUser(HttpSession session) {
        User user = Sessions.signedIn(session);
        return user != null && user.getRole() == User.UserRole.RIDER ? user : null;
    }

    @GetMapping("/rider")
    String deliveries(HttpSession session, Model model) {
        if (riderUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderService.getDeliveryQueue());
        return "rider";
    }

    @PostMapping("/rider/orders/{id}/advance")
    String advance(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        User user = riderUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            orderService.advanceStatus(id, user);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rider";
    }
}
