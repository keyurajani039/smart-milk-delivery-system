package com.example.milkdelivery.serviceImpl;

import com.example.milkdelivery.entity.MilkCategory;
import com.example.milkdelivery.exception.ResourceNotFoundException;
import com.example.milkdelivery.repository.MilkCategoryRepository;
import com.example.milkdelivery.service.MilkCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MilkCategoryServiceImpl implements MilkCategoryService {

    @Autowired
    private MilkCategoryRepository milkCategoryRepository;

    @Override
    @CacheEvict(value = "milkCategories", allEntries = true)
    public MilkCategory saveCategory(MilkCategory category) {
        if (category.getActive() == null) {
            category.setActive(true);
        }
        return milkCategoryRepository.save(category);
    }

    @Override
    @Cacheable(value = "milkCategories")
    public List<MilkCategory> getAllCategories() {
        return milkCategoryRepository.findAll();
    }

    @Override
    @CacheEvict(value = "milkCategories", allEntries = true)
    public MilkCategory updateCategory(Long id, MilkCategory category) {
        MilkCategory existing = milkCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milk category not found"));

        existing.setCategoryName(category.getCategoryName());
        existing.setPricePerLiter(category.getPricePerLiter());
        return milkCategoryRepository.save(existing);
    }

    @Override
    @CacheEvict(value = "milkCategories", allEntries = true)
    public String deleteCategory(Long id) {
        if (!milkCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Milk category not found");
        }
        milkCategoryRepository.deleteById(id);
        return "Milk category deleted permanently";
    }

    @Override
    @CacheEvict(value = "milkCategories", allEntries = true)
    public String changeStatus(Long id, Boolean active) {
        MilkCategory category = milkCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milk category not found"));

        category.setActive(active);
        milkCategoryRepository.save(category);
        return active ? "Milk category activated successfully" : "Milk category deactivated successfully";
    }
}