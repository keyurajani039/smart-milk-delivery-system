package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.MilkCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MilkCategoryRepository
        extends JpaRepository<MilkCategory, Long> {

    List<MilkCategory> findByCategoryName(String categoryName);
}