package com.dailyrupi.masterdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_category_id", nullable = false)
    private Long subCategoryId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "is_default", nullable = false)
    private boolean defaultEntry;

    @Column(nullable = false)
    private boolean active = true;

    protected Item() {
    }

    public Item(Long subCategoryId, String name) {
        this.subCategoryId = subCategoryId;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Long getSubCategoryId() {
        return subCategoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefaultEntry() {
        return defaultEntry;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
