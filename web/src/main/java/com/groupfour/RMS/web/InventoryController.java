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

@Controller
public class InventoryController {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    @GetMapping("/manager/inventory")
    String inventory(HttpSession session, Model model) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("items", menuItemDAO.getAllMenuItems());
        return "inventory";
    }

    @PostMapping("/manager/inventory/{id}/adjust")
    String adjust(@PathVariable int id,
                  @RequestParam int quantity,
                  @RequestParam String action,
                  HttpSession session,
                  RedirectAttributes redirect) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        if (quantity <= 0) {
            redirect.addFlashAttribute("error", "Quantity must be more than 0.");
            return "redirect:/manager/inventory";
        }
        if (!"add".equals(action) && !"remove".equals(action)) {
            redirect.addFlashAttribute("error", "Choose add or remove.");
            return "redirect:/manager/inventory";
        }
        MenuItem item = menuItemDAO.getMenuItemById(id);
        if (item == null) {
            redirect.addFlashAttribute("error", "That item no longer exists.");
            return "redirect:/manager/inventory";
        }
        int change = "remove".equals(action) ? -quantity : quantity;
        if (menuItemDAO.updateStock(id, change)) {
            redirect.addFlashAttribute("message", "Stock updated for " + item.getName() + ".");
        } else {
            redirect.addFlashAttribute("error", "Stock cannot go below 0 for " + item.getName() + ".");
        }
        return "redirect:/manager/inventory";
    }
}
