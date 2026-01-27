package com.group4.ecommerceplatform.dto.category;

import jakarta.validation.constraints.NotEmpty;

public class CategoryDTO {
    private String id;

    @NotEmpty(message = "Name is required")
    private String name;

    @NotEmpty(message = "Description is required")
    private String description;
}
