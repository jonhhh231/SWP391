package com.group4.ecommerceplatform.apis;

import com.group4.ecommerceplatform.dto.product.ProductDTO;
import com.group4.ecommerceplatform.entities.Product;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products")
public class ProductAPI {
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody @Valid ProductDTO dto)
    {
        return null;
    }
}
