package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class MenuController {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    @GetMapping("/manager/menu")
    String menu(HttpSession session, Model model) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("items", menuItemDAO.getAllMenuItems());
        return "menu";
    }

    @PostMapping("/manager/menu")
    String addItem(@RequestParam String name,
                   @RequestParam String category,
                   @RequestParam BigDecimal price,
                   @RequestParam int stock,
                   @RequestParam int threshold,
                   HttpSession session,
                   RedirectAttributes redirect) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        String problem = validate(name, price, stock, threshold);
        if (problem != null) {
            redirect.addFlashAttribute("error", problem);
            return "redirect:/manager/menu";
        }
        MenuItem item = new MenuItem(0, name.trim(), category.trim(), price, stock, threshold, true, null);
        if (menuItemDAO.addMenuItem(item)) {
            redirect.addFlashAttribute("message", "Item added.");
        } else {
            redirect.addFlashAttribute("error", "The item could not be added.");
        }
        return "redirect:/manager/menu";
    }

    @PostMapping("/manager/menu/{id}")
    String updateItem(@PathVariable int id,
                      @RequestParam String name,
                      @RequestParam String category,
                      @RequestParam BigDecimal price,
                      @RequestParam int stock,
                      @RequestParam int threshold,
                      @RequestParam(defaultValue = "false") boolean available,
                      HttpSession session,
                      RedirectAttributes redirect) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        MenuItem existing = menuItemDAO.getMenuItemById(id);
        if (existing == null) {
            redirect.addFlashAttribute("error", "That item no longer exists.");
            return "redirect:/manager/menu";
        }
        String problem = validate(name, price, stock, threshold);
        if (problem != null) {
            redirect.addFlashAttribute("error", problem);
            return "redirect:/manager/menu";
        }
        MenuItem item = new MenuItem(id, name.trim(), category.trim(), price, stock, threshold, available, existing.getCreatedAt());
        if (menuItemDAO.updateMenuItem(item)) {
            redirect.addFlashAttribute("message", "Saved " + item.getName() + ".");
        } else {
            redirect.addFlashAttribute("error", "The item could not be saved.");
        }
        return "redirect:/manager/menu";
    }

    private String validate(String name, BigDecimal price, int stock, int threshold) {
        if (name == null || name.isBlank()) {
            return "Name is required.";
        }
        if (price.signum() < 0) {
            return "Price cannot be negative.";
        }
        if (stock < 0 || threshold < 0) {
            return "Stock and low-stock level cannot be negative.";
        }
        return null;
    }
}
