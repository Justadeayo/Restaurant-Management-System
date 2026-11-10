package com.groupfour.RMS.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class ReportsController {

    @GetMapping("/manager/reports")
    String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                   HttpSession session,
                   Model model) {
        if (Sessions.manager(session) == null) {
            return "redirect:/login";
        }
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(7);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        if (start.isAfter(end)) {
            model.addAttribute("error", "The start date must be before the end date.");
            return "reports";
        }
        var daily = ReportQueries.dailySales(start, end);
        BigDecimal total = daily.stream()
                .map(ReportQueries.DailySales::totalSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("daily", daily);
        model.addAttribute("total", total);
        model.addAttribute("payments", ReportQueries.paymentSales(start, end));
        model.addAttribute("items", ReportQueries.itemSales(start, end));
        return "reports";
    }
}
