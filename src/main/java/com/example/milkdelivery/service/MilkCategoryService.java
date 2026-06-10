package com.example.milkdelivery.service;

import com.example.milkdelivery.entity.MilkCategory;

import java.util.List;

public interface MilkCategoryService {

    MilkCategory saveCategory(
            MilkCategory category
    );

    List<MilkCategory> getAllCategories();

    MilkCategory updateCategory(
            Long id,
            MilkCategory category
    );

    String deleteCategory(Long id);

    String changeStatus(Long id, Boolean active);
}