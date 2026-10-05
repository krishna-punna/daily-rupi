package com.dailyrupi.masterdata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dailyrupi.common.ApiException;
import com.dailyrupi.expense.ExpenseRepository;
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
    private final ExpenseRepository expenses;

    public MasterDataService(CategoryRepository categories, SubCategoryRepository subCategories,
            ItemRepository items, ExpenseRepository expenses) {
        this.categories = categories;
        this.subCategories = subCategories;
        this.items = items;
        this.expenses = expenses;
    }

    /**
     * The Category > Sub Category > Item tree, built from three queries.
     *
     * @param includeInactive false for the expense dropdowns: inactive entries are
     *        left out, and so is everything beneath an inactive parent
     */
    @Transactional(readOnly = true)
    public List<CategoryNode> tree(boolean includeInactive) {
        Map<Long, List<ItemNode>> itemsBySubCategory = items.findAllByOrderByIdAsc().stream()
                .filter(i -> includeInactive || i.isActive())
                .collect(Collectors.groupingBy(Item::getSubCategoryId, Collectors.mapping(
                        MasterDataService::node, Collectors.toList())));

        Map<Long, List<SubCategoryNode>> subCategoriesByCategory = subCategories.findAllByOrderByIdAsc().stream()
                .filter(s -> includeInactive || s.isActive())
                .collect(Collectors.groupingBy(SubCategory::getCategoryId, Collectors.mapping(
                        s -> node(s, itemsBySubCategory.getOrDefault(s.getId(), List.of())),
                        Collectors.toList())));

        return categories.findAllByOrderBySortOrderAscNameAsc().stream()
                .filter(c -> includeInactive || c.isActive())
                .map(c -> node(c, subCategoriesByCategory.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    // ---- categories ----

    @Transactional
    public CategoryNode addCategory(String rawName) {
        String name = clean(rawName);
        if (categories.existsByNameIgnoreCase(name)) {
            throw duplicate("category");
        }
        return node(categories.save(new Category(name)), List.of());
    }

    @Transactional
    public CategoryNode renameCategory(Long id, String rawName) {
        Category category = category(id);
        String name = clean(rawName);
        if (!name.equalsIgnoreCase(category.getName()) && categories.existsByNameIgnoreCase(name)) {
            throw duplicate("category");
        }
        category.setName(name);
        return node(category, List.of());
    }

    @Transactional
    public CategoryNode setCategoryActive(Long id, boolean active) {
        Category category = category(id);
        category.setActive(active);
        return node(category, List.of());
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = category(id);
        if (subCategories.existsByCategoryId(id)) {
            throw inUse("This category still has sub categories. Delete them first, or mark it inactive.");
        }
        categories.delete(category);
    }

    // ---- sub categories ----

    @Transactional
    public SubCategoryNode addSubCategory(Long categoryId, String rawName) {
        category(categoryId);
        String name = clean(rawName);
        if (subCategories.existsByCategoryIdAndNameIgnoreCase(categoryId, name)) {
            throw duplicate("sub category");
        }
        return node(subCategories.save(new SubCategory(categoryId, name)), List.of());
    }

    @Transactional
    public SubCategoryNode renameSubCategory(Long id, String rawName) {
        SubCategory subCategory = subCategory(id);
        String name = clean(rawName);
        if (!name.equalsIgnoreCase(subCategory.getName())
                && subCategories.existsByCategoryIdAndNameIgnoreCase(subCategory.getCategoryId(), name)) {
            throw duplicate("sub category");
        }
        subCategory.setName(name);
        return node(subCategory, List.of());
    }

    @Transactional
    public SubCategoryNode setSubCategoryActive(Long id, boolean active) {
        SubCategory subCategory = subCategory(id);
        subCategory.setActive(active);
        return node(subCategory, List.of());
    }

    @Transactional
    public void deleteSubCategory(Long id) {
        SubCategory subCategory = subCategory(id);
        if (items.existsBySubCategoryId(id)) {
            throw inUse("This sub category still has items. Delete them first, or mark it inactive.");
        }
        subCategories.delete(subCategory);
    }

    // ---- items ----

    @Transactional
    public ItemNode addItem(Long subCategoryId, String rawName) {
        subCategory(subCategoryId);
        String name = clean(rawName);
        if (items.existsBySubCategoryIdAndNameIgnoreCase(subCategoryId, name)) {
            throw duplicate("item");
        }
        return node(items.save(new Item(subCategoryId, name)));
    }

    @Transactional
    public ItemNode renameItem(Long id, String rawName) {
        Item item = item(id);
        String name = clean(rawName);
        if (!name.equalsIgnoreCase(item.getName())
                && items.existsBySubCategoryIdAndNameIgnoreCase(item.getSubCategoryId(), name)) {
            throw duplicate("item");
        }
        item.setName(name);
        return node(item);
    }

    @Transactional
    public ItemNode setItemActive(Long id, boolean active) {
        Item item = item(id);
        item.setActive(active);
        return node(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = item(id);
        if (expenses.existsByItemId(id)) {
            throw inUse("Expenses are recorded against this item. Mark it inactive instead.");
        }
        items.delete(item);
    }

    // ---- helpers ----

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> notFound("Category"));
    }

    private SubCategory subCategory(Long id) {
        return subCategories.findById(id).orElseThrow(() -> notFound("Sub category"));
    }

    private Item item(Long id) {
        return items.findById(id).orElseThrow(() -> notFound("Item"));
    }

    private static ItemNode node(Item i) {
        return new ItemNode(i.getId(), i.getName(), i.isDefaultEntry(), i.isActive());
    }

    private static SubCategoryNode node(SubCategory s, List<ItemNode> children) {
        return new SubCategoryNode(s.getId(), s.getName(), s.isDefaultEntry(), s.isActive(), children);
    }

    private static CategoryNode node(Category c, List<SubCategoryNode> children) {
        return new CategoryNode(c.getId(), c.getName(), c.isDefaultEntry(), c.isActive(), children);
    }

    private static String clean(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", what + " not found");
    }

    private static ApiException duplicate(String what) {
        return new ApiException(HttpStatus.CONFLICT, "DUPLICATE", "A " + what + " with this name already exists");
    }

    private static ApiException inUse(String message) {
        return new ApiException(HttpStatus.CONFLICT, "IN_USE", message);
    }
}
