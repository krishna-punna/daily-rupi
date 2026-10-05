package com.dailyrupi.masterdata;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findByActiveTrueOrderByIdAsc();

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}
