package com.dailyrupi.masterdata;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class MasterDataDtos {

    private MasterDataDtos() {
    }

    /** Body for adding or renaming a category, sub category or item. */
    public record NameRequest(
            @NotBlank
            @Size(max = 80)
            // Letters, digits and everyday punctuation only: no markup, no control characters.
            @Pattern(regexp = "^[\\p{L}\\p{N} &/().,'+-]+$", message = "contains characters that are not allowed")
            String name) {
    }

    /** Body for switching an entry between active and inactive. */
    public record StatusRequest(@NotNull Boolean active) {
    }

    public record ItemNode(Long id, String name, boolean isDefault, boolean active) {
    }

    public record SubCategoryNode(Long id, String name, boolean isDefault, boolean active, List<ItemNode> items) {
    }

    public record CategoryNode(Long id, String name, boolean isDefault, boolean active,
            List<SubCategoryNode> subCategories) {
    }
}
