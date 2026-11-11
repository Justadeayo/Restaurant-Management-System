package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.dao.OrderDAO;
import com.groupfour.RMS.dao.PaymentDAO;
import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
import com.groupfour.RMS.model.Payment;
import com.groupfour.RMS.model.User;
import com.groupfour.RMS.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WebController {
    private final UserDAO userDAO = new UserDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final OrderService orderService = new OrderService();

    private static final List<User.UserRole> STAFF_ROLES = List.of(User.UserRole.MANAGER, User.UserRole.CASHIER, User.UserRole.KITCHEN);

    private User currentUser(HttpSession session) {
        if (session.getAttribute("user") instanceof User user && STAFF_ROLES.contains(user.getRole())) {
            return user;
        }
        return null;
    }

    // Order pages are for cashiers and managers only; kitchen staff use /kitchen.
    private User cashierUser(HttpSession session) {
        User user = currentUser(session);
        return user != null && user.getRole() != User.UserRole.KITCHEN ? user : null;
    }

    private String homePage(User user) {
        if (user.getRole() == User.UserRole.MANAGER) {
            return "redirect:/manager";
        }
        if (user.getRole() == User.UserRole.KITCHEN) {
            return "redirect:/kitchen";
        }
        return "redirect:/order/new";
    }

    @GetMapping("/")
    String root(HttpSession session) {
        User user = cashierUser(session);
        return user == null ? "redirect:/login" : homePage(user);
    }

    @GetMapping("/login")
    String loginForm(HttpSession session) {
        User user = cashierUser(session);
        return user == null ? "login" : homePage(user);
    }

    @PostMapping("/login")
    String login(@RequestParam String username,
                 @RequestParam String password,
                 HttpSession session,
                 Model model) {
        User user = userDAO.authenticate(username, password);
        if (user == null || !STAFF_ROLES.contains(user.getRole())) {
            model.addAttribute("error", "Invalid username or password.");
            return "login";
        }
        session.setAttribute("user", user);
        return homePage(user);
    }

    @PostMapping("/logout")
    String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/order/new")
    String newOrderForm(HttpSession session, Model model) {
        if (cashierUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
        return "order-new";
    }

    @PostMapping("/order/new")
    String createOrder(@RequestParam("menuItemId") List<Integer> menuItemIds,
                       @RequestParam("quantity") List<Integer> quantities,
                       @RequestParam(defaultValue = "TABLE") String orderType,
                       @RequestParam(required = false, value = "tableNumber") String tableNumber,
                       @RequestParam(required = false, value = "deliveryAddress") String deliveryAddress,
                       HttpSession session,
                       Model model) {
        User user = cashierUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        Order order = new Order();
        for (int i = 0; i < menuItemIds.size() && i < quantities.size(); i++) {
            int quantity = quantities.get(i);
            if (quantity <= 0) {
                continue;
            }
            MenuItem menuItem = menuItemDAO.getMenuItemById(menuItemIds.get(i));
            if (menuItem == null) {
                continue;
            }
            order.addOrderItem(new OrderItem(menuItem.getId(), menuItem.getName(), quantity, menuItem.getPrice()));
        }

        if (order.getOrderItems().isEmpty()) {
            model.addAttribute("error", "Add at least one item to the order.");
            model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
            return "order-new";
        }

        try {
            order.setOrderType(Order.OrderType.valueOf(orderType));
            order.setTableNumber(tableNumber);
            order.setDeliveryAddress(deliveryAddress);
            int orderId = orderService.createOrder(order, user);
            return "redirect:/orders/" + orderId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
            return "order-new";
        }
    }

    @GetMapping("/orders")
    String orders(HttpSession session, Model model) {
        if (cashierUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders";
    }

    @GetMapping("/orders/{id}")
    String orderDetail(@PathVariable int id, HttpSession session, Model model) {
        if (cashierUser(session) == null) {
            return "redirect:/login";
        }
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("lines", orderDAO.getOrderItems(id));
        model.addAttribute("paid", paymentDAO.hasSuccessfulPayment(id));
        model.addAttribute("canPay", order.getStatus() == Order.OrderStatus.DELIVERED);
        model.addAttribute("next", order.nextStatus());
        model.addAttribute("methods", Payment.PaymentMethod.values());
        return "order";
    }

    @PostMapping("/orders/{id}/advance")
    String advance(@PathVariable int id,
                   HttpSession session,
                   RedirectAttributes redirect) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            orderService.advanceStatus(id, user);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/orders/{id}/pay")
    String pay(@PathVariable int id,
               @RequestParam Payment.PaymentMethod method,
               HttpSession session,
               RedirectAttributes redirect) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            if (!orderService.processPayment(id, method, user)) {
                redirect.addFlashAttribute("error", "Payment could not be recorded.");
            }
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
