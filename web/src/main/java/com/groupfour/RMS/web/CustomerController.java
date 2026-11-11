package com.groupfour.RMS.web;

import com.groupfour.RMS.dao.MenuItemDAO;
import com.groupfour.RMS.dao.OrderDAO;
import com.groupfour.RMS.dao.UserDAO;
import com.groupfour.RMS.model.MenuItem;
import com.groupfour.RMS.model.Order;
import com.groupfour.RMS.model.OrderItem;
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

// Pages for customers: sign up, order online for delivery, and follow their own orders.
@Controller
public class CustomerController {
    private final UserDAO userDAO = new UserDAO();
    private final MenuItemDAO menuItemDAO = new MenuItemDAO();
    private final OrderService orderService = new OrderService();
    private final OrderDAO orderDAO = new OrderDAO();

    private User customerUser(HttpSession session) {
        User user = Sessions.signedIn(session);
        return user != null && user.getRole() == User.UserRole.CUSTOMER ? user : null;
    }

    @GetMapping("/signup")
    String signupForm() {
        return "signup";
    }

    @PostMapping("/signup")
    String signup(@RequestParam String username,
                  @RequestParam String password,
                  @RequestParam String confirmPassword,
                  Model model) {
        if (username.isBlank() || password.length() < 6) {
            model.addAttribute("error", "Choose a username and a password of at least 6 characters.");
            return "signup";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "The two passwords do not match.");
            return "signup";
        }
        if (!userDAO.createUser(username.trim(), password, User.UserRole.CUSTOMER)) {
            model.addAttribute("error", "That username is already taken.");
            return "signup";
        }
        return "redirect:/login?created";
    }

    @GetMapping("/shop")
    String shop(HttpSession session, Model model) {
        if (customerUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
        return "shop";
    }

    @PostMapping("/shop/order")
    String placeOrder(@RequestParam("menuItemId") List<Integer> menuItemIds,
                      @RequestParam("quantity") List<Integer> quantities,
                      @RequestParam(required = false, value = "deliveryAddress") String deliveryAddress,
                      HttpSession session,
                      Model model,
                      RedirectAttributes redirect) {
        User customer = customerUser(session);
        if (customer == null) {
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
            model.addAttribute("error", "Add at least one item to your order.");
            model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
            return "shop";
        }

        try {
            order.setDeliveryAddress(deliveryAddress);
            int orderId = orderService.createOrder(order, customer);
            redirect.addFlashAttribute("placed", true);
            return "redirect:/my-orders/" + orderId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("items", menuItemDAO.getAvailableMenuItems());
            return "shop";
        }
    }

    @GetMapping("/my-orders")
    String myOrders(HttpSession session, Model model) {
        User customer = customerUser(session);
        if (customer == null) {
            return "redirect:/login";
        }
        model.addAttribute("orders", orderService.getCustomerOrders(customer));
        return "my-orders";
    }

    @PostMapping("/my-orders/{id}/pay")
    String payOrder(@PathVariable int id, HttpSession session, RedirectAttributes redirect) {
        User customer = customerUser(session);
        if (customer == null) {
            return "redirect:/login";
        }
        try {
            orderService.customerPay(id, customer);
            redirect.addFlashAttribute("paid", true);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my-orders/" + id;
    }

    @GetMapping("/my-orders/{id}")
    String myOrder(@PathVariable int id, HttpSession session, Model model) {
        User customer = customerUser(session);
        if (customer == null) {
            return "redirect:/login";
        }
        Order order = orderService.getCustomerOrder(id, customer);
        if (order == null) {
            return "redirect:/my-orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("steps", order.progressSteps());
        model.addAttribute("items", orderDAO.getOrderItems(id));
        return "my-order";
    }
}
