package com.helps.domain.service;

import com.helps.domain.model.Category;
import com.helps.domain.repository.CategoryRepository;
import com.helps.dto.CategoryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    public List<CategoryDto> getAllCategories() {
        logger.debug("Buscando todas as categorias");
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> getActiveCategories() {
        logger.debug("Buscando categorias ativas");
        List<Category> categories = categoryRepository.findByActiveTrue();
        return categories.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CategoryDto getCategoryById(Long id) {
        logger.debug("Buscando categoria por ID: {}", id);
        return categoryRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + id));
    }

    @Transactional
    public CategoryDto createCategory(CategoryDto categoryDto) {
        if (categoryRepository.findByName(categoryDto.name()).isPresent()) {
            logger.warn("Tentativa de criar categoria com nome já existente: {}", categoryDto.name());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Já existe uma categoria com este nome");
        }

        Category category = new Category();
        category.setName(categoryDto.name());
        category.setDescription(categoryDto.description());
        category.setActive(categoryDto.active());

        Category savedCategory = categoryRepository.save(category);
        logger.info("Categoria criada com sucesso: {}", savedCategory.getName());
        return toDto(savedCategory);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + id));

        categoryRepository.findByName(categoryDto.name())
                .ifPresent(existingCategory -> {
                    if (!existingCategory.getId().equals(id)) {
                        logger.warn("Tentativa de atualizar categoria para um nome já em uso: {}", categoryDto.name());
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Já existe uma categoria com este nome");
                    }
                });

        category.setName(categoryDto.name());
        category.setDescription(categoryDto.description());
        category.setActive(categoryDto.active());

        Category updatedCategory = categoryRepository.save(category);
        logger.info("Categoria atualizada com sucesso: {}", updatedCategory.getName());
        return toDto(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + id));

        categoryRepository.delete(category);
        logger.info("Categoria excluída com sucesso: {}", category.getName());
    }

    @Transactional
    public CategoryDto toggleCategoryActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada com ID: " + id));

        category.setActive(!category.isActive());
        Category updatedCategory = categoryRepository.save(category);

        String status = category.isActive() ? "ativada" : "desativada";
        logger.info("Categoria {} com sucesso: {}", status, category.getName());

        return toDto(updatedCategory);
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive()
        );
    }
}