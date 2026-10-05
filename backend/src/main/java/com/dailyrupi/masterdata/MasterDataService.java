package com.dailyrupi.masterdata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dailyrupi.common.ApiException;
import com.dailyrupi.masterdata.MasterDataDtos.CategoryNode;
import com.dailyrupi.masterdata.MasterDataDtos.ItemNode;
import com.dailyrupi.masterdata.MasterDataDtos.SubCategoryNode;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MasterDataService {

    private final CategoryRepository categories;
    private final SubCategoryRepository subCategories;
    private final ItemRepository items;

    public MasterDataService(CategoryRepository categories, SubCategoryRepository subCategories,
            ItemRepository items) {
        this.categories = categories;
        this.subCategories = subCategories;
        this.items = items;
    }

    /** The whole Category > Sub Category > Item tree, built from three queries. */
    @Transactional(readOnly = true)
    public List<CategoryNode> tree() {
        Map<Long, List<ItemNode>> itemsBySubCategory = items.findByActiveTrueOrderByIdAsc().stream()
                .collect(Collectors.groupingBy(Item::getSubCategoryId, Collectors.mapping(
                        i -> new ItemNode(i.getId(), i.getName(), i.isDefaultEntry()), Collectors.toList())));

        Map<Long, List<SubCategoryNode>> subCategoriesByCategory = subCategories.findByActiveTrueOrderByIdAsc()
                .stream()
                .collect(Collectors.groupingBy(SubCategory::getCategoryId, Collectors.mapping(
                        s -> new SubCategoryNode(s.getId(), s.getName(), s.isDefaultEntry(),
                                itemsBySubCategory.getOrDefault(s.getId(), List.of())),
                        Collectors.toList())));

        return categories.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(c -> new CategoryNode(c.getId(), c.getName(), c.isDefaultEntry(),
                        subCategoriesByCategory.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public CategoryNode addCategory(String rawName) {
        String name = clean(rawName);
        if (categories.existsByNameIgnoreCase(name)) {
            throw duplicate("category");
        }
        Category saved = categories.save(new Category(name));
        return new CategoryNode(saved.getId(), saved.getName(), false, List.of());
    }

    @Transactional
    public SubCategoryNode addSubCategory(Long categoryId, String rawName) {
        String name = clean(rawName);
        if (!categories.existsById(categoryId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Category not found");
        }
        if (subCategories.existsByCategoryIdAndNameIgnoreCase(categoryId, name)) {
            throw duplicate("sub category");
        }
        SubCategory saved = subCategories.save(new SubCategory(categoryId, name));
        return new SubCategoryNode(saved.getId(), saved.getName(), false, List.of());
    }

    @Transactional
    public ItemNode addItem(Long subCategoryId, String rawName) {
        String name = clean(rawName);
        if (!subCategories.existsById(subCategoryId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Sub category not found");
        }
        if (items.existsBySubCategoryIdAndNameIgnoreCase(subCategoryId, name)) {
            throw duplicate("item");
        }
        Item saved = items.save(new Item(subCategoryId, name));
        return new ItemNode(saved.getId(), saved.getName(), false);
    }

    private static String clean(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static ApiException duplicate(String what) {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE", "A " + what + " with this name already exists");
    }
}
