package com.dailyrupi.masterdata;

import java.util.List;

import com.dailyrupi.masterdata.MasterDataDtos.CategoryNode;
import com.dailyrupi.masterdata.MasterDataDtos.ItemNode;
import com.dailyrupi.masterdata.MasterDataDtos.NameRequest;
import com.dailyrupi.masterdata.MasterDataDtos.SubCategoryNode;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Needs the USER role, enforced by the /api/** rule in SecurityConfig. */
@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataService service;

    public MasterDataController(MasterDataService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryNode> tree() {
        return service.tree();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryNode addCategory(@Valid @RequestBody NameRequest body) {
        return service.addCategory(body.name());
    }

    @PostMapping("/categories/{categoryId}/sub-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public SubCategoryNode addSubCategory(@PathVariable Long categoryId, @Valid @RequestBody NameRequest body) {
        return service.addSubCategory(categoryId, body.name());
    }

    @PostMapping("/sub-categories/{subCategoryId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemNode addItem(@PathVariable Long subCategoryId, @Valid @RequestBody NameRequest body) {
        return service.addItem(subCategoryId, body.name());
    }
}
