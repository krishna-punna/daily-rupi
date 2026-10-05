package com.dailyrupi.masterdata;

import java.util.List;

import com.dailyrupi.masterdata.MasterDataDtos.CategoryNode;
import com.dailyrupi.masterdata.MasterDataDtos.ItemNode;
import com.dailyrupi.masterdata.MasterDataDtos.NameRequest;
import com.dailyrupi.masterdata.MasterDataDtos.StatusRequest;
import com.dailyrupi.masterdata.MasterDataDtos.SubCategoryNode;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** Active entries only by default (for dropdowns); the management screen asks for everything. */
    @GetMapping
    public List<CategoryNode> tree(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.tree(includeInactive);
    }

    // ---- categories ----

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryNode addCategory(@Valid @RequestBody NameRequest body) {
        return service.addCategory(body.name());
    }

    @PutMapping("/categories/{id}")
    public CategoryNode renameCategory(@PathVariable Long id, @Valid @RequestBody NameRequest body) {
        return service.renameCategory(id, body.name());
    }

    @PatchMapping("/categories/{id}/status")
    public CategoryNode setCategoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return service.setCategoryActive(id, body.active());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
    }

    // ---- sub categories ----

    @PostMapping("/categories/{categoryId}/sub-categories")
    @ResponseStatus(HttpStatus.CREATED)
    public SubCategoryNode addSubCategory(@PathVariable Long categoryId, @Valid @RequestBody NameRequest body) {
        return service.addSubCategory(categoryId, body.name());
    }

    @PutMapping("/sub-categories/{id}")
    public SubCategoryNode renameSubCategory(@PathVariable Long id, @Valid @RequestBody NameRequest body) {
        return service.renameSubCategory(id, body.name());
    }

    @PatchMapping("/sub-categories/{id}/status")
    public SubCategoryNode setSubCategoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return service.setSubCategoryActive(id, body.active());
    }

    @DeleteMapping("/sub-categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubCategory(@PathVariable Long id) {
        service.deleteSubCategory(id);
    }

    // ---- items ----

    @PostMapping("/sub-categories/{subCategoryId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemNode addItem(@PathVariable Long subCategoryId, @Valid @RequestBody NameRequest body) {
        return service.addItem(subCategoryId, body.name());
    }

    @PutMapping("/items/{id}")
    public ItemNode renameItem(@PathVariable Long id, @Valid @RequestBody NameRequest body) {
        return service.renameItem(id, body.name());
    }

    @PatchMapping("/items/{id}/status")
    public ItemNode setItemStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        return service.setItemActive(id, body.active());
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
    }
}
