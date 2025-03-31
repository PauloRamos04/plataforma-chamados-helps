package com.helps.controller;

import com.helps.domain.service.CategoryService;
import com.helps.dto.CategoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Gerenciamento de Categorias", description = "APIs para gerenciar categorias de tickets")
public class CategoryController {
    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        logger.debug("Listando todas as categorias");
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/active")
    @Operation(summary = "Listar categorias ativas")
    public ResponseEntity<List<CategoryDto>> getActiveCategories() {
        logger.debug("Listando categorias ativas");
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por ID")
    public ResponseEntity<CategoryDto> getCategoryById(@PathVariable Long id) {
        logger.debug("Buscando categoria com ID: {}", id);
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Criar nova categoria")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryDto categoryDto) {
        logger.info("Criando nova categoria: {}", categoryDto.name());
        CategoryDto createdCategory = categoryService.createCategory(categoryDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Atualizar categoria existente")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto categoryDto) {
        logger.info("Atualizando categoria com ID: {}", id);
        CategoryDto updatedCategory = categoryService.updateCategory(id, categoryDto);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Excluir categoria")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        logger.info("Excluindo categoria com ID: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Ativar/Desativar categoria")
    public ResponseEntity<CategoryDto> toggleCategoryActive(@PathVariable Long id) {
        logger.info("Alternando estado de ativação da categoria com ID: {}", id);
        CategoryDto updatedCategory = categoryService.toggleCategoryActive(id);
        return ResponseEntity.ok(updatedCategory);
    }
}