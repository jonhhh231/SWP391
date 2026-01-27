package com.group4.ecommerceplatform.controllers;

import ch.qos.logback.core.model.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/category")
public class CategoryController {
    @GetMapping
    public String getCategoryListPage(Model model) {
        return "admin/category-list";
    }

    @GetMapping("/create")
    public String getCategoryCreatePage(Model model) {
        return "admin/pages/category-create";
    }

    @GetMapping("/edit")
    public String getCategoryEditPage(Model model) {
        return "admin/pages/category-edit";
    }

    @GetMapping("/update")
    public String getCategoryUpdate(Model model) {
        return "admin/pages/category-update";
    }
}
