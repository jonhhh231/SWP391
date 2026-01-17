package com.group4.ecommerceplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @GetMapping("/admin/dashboard")
    public String dashboard(){
        return "admin/pages/dashboard";
    }
}
