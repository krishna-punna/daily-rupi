package com.dailyrupi.masterdata;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByActiveTrueOrderByIdAsc();

    boolean existsBySubCategoryIdAndNameIgnoreCase(Long subCategoryId, String name);
}
