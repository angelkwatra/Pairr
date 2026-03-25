package com.connect.pairr.mapper;

import com.connect.pairr.model.dto.AddCategoryRequest;
import com.connect.pairr.model.dto.CategoryResponse;
import com.connect.pairr.model.entity.Category;

public class CategoryMapper {

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }

    public static Category toEntity(AddCategoryRequest request) {
        return Category.builder()
                .name(request.name())
                .build();
    }
}
