package com.group4.ecommerceplatform.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/products")
public class ProductController {
    @GetMapping
    public String getProductListPage(Model model){
        return "admin/pages/product-list";
    }

    @GetMapping("/create")
    public String getProductCreatePage(Model model){
        return "admin/pages/product-create";
    }

    @GetMapping("/edit")
    public String getProductUpdatePage(Model model){
        return "admin/pages/product-update";
    }
}
