package sneak_shop.controller.admin;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sneak_shop.common.response.ApiResponse;
import sneak_shop.dto.request.CategoryRequest;
import sneak_shop.dto.response.CategoryResponse;
import sneak_shop.service.impl.CategoryServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
public class AdminCategoryController {

    private final CategoryServiceImpl categoryService;

    public AdminCategoryController(CategoryServiceImpl categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll(
            @RequestParam(value = "deleted", required = false) Boolean deleted) {
        return ApiResponse.ok(categoryService.adminGetAll(deleted));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok("Tao danh muc thanh cong", categoryService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Integer id, @Valid @RequestBody CategoryRequest req) {
        return ApiResponse.ok(categoryService.update(id, req));
    }

    @GetMapping("/{id}/product-count")
    public ApiResponse<Long> productCount(@PathVariable Integer id) {
        return ApiResponse.ok(categoryService.countProducts(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Integer id,
                                     @RequestParam(value = "moveProductsTo", required = false) Integer moveProductsTo) {
        categoryService.delete(id, moveProductsTo);
        return ApiResponse.ok("Xoa danh muc thanh cong");
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable Integer id) {
        categoryService.restore(id);
        return ApiResponse.ok("Khoi phuc danh muc thanh cong");
    }
}
