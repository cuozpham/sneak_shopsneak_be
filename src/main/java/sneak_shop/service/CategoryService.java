package sneak_shop.service;

import sneak_shop.dto.request.CategoryRequest;
import sneak_shop.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    List<CategoryResponse> getRoots();
    CategoryResponse getBySlug(String slug);
    CategoryResponse create(CategoryRequest req);
    CategoryResponse update(Integer id, CategoryRequest req);
    long countProducts(Integer id);
    void delete(Integer id, Integer moveProductsToId);
    void restore(Integer id);
}
