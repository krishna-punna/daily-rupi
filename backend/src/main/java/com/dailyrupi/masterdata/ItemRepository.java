package com.dailyrupi.masterdata;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByIdAsc();

    boolean existsBySubCategoryId(Long subCategoryId);

    boolean existsBySubCategoryIdAndNameIgnoreCase(Long subCategoryId, String name);
}
