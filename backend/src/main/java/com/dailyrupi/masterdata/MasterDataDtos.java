package com.dailyrupi.masterdata;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MasterDataDtos {

    private MasterDataDtos() {
    }

    /** Body for adding a custom category, sub category or item. */
    public record NameRequest(
            @NotBlank
            @Size(max = 80)
            // Letters, digits and everyday punctuation only: no markup, no control characters.
            @Pattern(regexp = "^[\\p{L}\\p{N} &/().,'+-]+$", message = "contains characters that are not allowed")
            String name) {
    }

    public record ItemNode(Long id, String name, boolean isDefault) {
    }

    public record SubCategoryNode(Long id, String name, boolean isDefault, List<ItemNode> items) {
    }

    public record CategoryNode(Long id, String name, boolean isDefault, List<SubCategoryNode> subCategories) {
    }
}
