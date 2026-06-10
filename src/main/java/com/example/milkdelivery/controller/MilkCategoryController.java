package com.example.milkdelivery.controller;

import com.example.milkdelivery.entity.MilkCategory;
import com.example.milkdelivery.service.MilkCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milk-categories")
public class MilkCategoryController {

    @Autowired
    private MilkCategoryService milkCategoryService;

    @PostMapping("/save")
    public MilkCategory saveCategory(
            @RequestBody MilkCategory category) {

        return milkCategoryService
                .saveCategory(category);
    }

    @GetMapping
    public List<MilkCategory> getAllCategories() {

        return milkCategoryService
                .getAllCategories();
    }

    @PutMapping("/update/{id}")
    public MilkCategory updateCategory(
            @PathVariable Long id,
            @RequestBody MilkCategory category) {

        return milkCategoryService
                .updateCategory(id, category);
    }

    @DeleteMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Long id) {

        return milkCategoryService
                .deleteCategory(id);
    }
    @PutMapping("/status/{id}")
    public String changeStatus(

            @PathVariable Long id,

            @RequestParam Boolean active) {

        return milkCategoryService
                .changeStatus(id, active);
    }
}