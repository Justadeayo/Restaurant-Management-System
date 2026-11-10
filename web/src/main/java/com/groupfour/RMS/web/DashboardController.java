package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.model.MenuItem;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();

    @GetMapping("/manager")
    String dashboard(HttpSession session, Model model) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        LocalDate today = LocalDate.now();
        List<ReportQueries.DailySales> daily = ReportQueries.dailySales(today, today);
        BigDecimal salesToday = daily.isEmpty() ? BigDecimal.ZERO : daily.get(0).totalSales();
        long ordersToday = daily.isEmpty() ? 0 : daily.get(0).orderCount();
        List<MenuItem> lowStock = menuItemDAO.getAllMenuItems().stream()
                .filter(MenuItem::isLowStock)
                .toList();
        model.addAttribute("today", today);
        model.addAttribute("salesToday", salesToday);
        model.addAttribute("ordersToday", ordersToday);
        model.addAttribute("openOrders", ReportQueries.pendingOrders());
        model.addAttribute("lowStock", lowStock);
        return "dashboard";
    }
}
