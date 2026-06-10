package com.example.milkdelivery.repository;

import com.example.milkdelivery.entity.MilkCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MilkCategoryRepository
        extends JpaRepository<MilkCategory, Long> {

}