package com.dailyrupi.masterdata;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

    List<SubCategory> findAllByOrderByIdAsc();

    boolean existsByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);
}
