package com.helps.domain.service;

import com.helps.domain.model.Category;
import com.helps.domain.model.Role;
import com.helps.domain.model.TicketType;
import com.helps.domain.repository.CategoryRepository;
import com.helps.domain.repository.RoleRepository;
import com.helps.domain.repository.TicketTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConfigCacheService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigCacheService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Cacheable(value = "categories")
    public List<Category> getAllCategories() {
        logger.debug("Buscando todas as categorias do banco de dados");
        return categoryRepository.findAll();
    }

    @Cacheable(value = "activeCategories")
    public List<Category> getActiveCategories() {
        logger.debug("Buscando categorias ativas do banco de dados");
        return categoryRepository.findByActiveTrue();
    }

    @Cacheable(value = "categories", key = "#id")
    public Optional<Category> getCategoryById(Long id) {
        logger.debug("Buscando categoria por ID do banco de dados: {}", id);
        return categoryRepository.findById(id);
    }

    @Cacheable(value = "categories", key = "#name")
    public Optional<Category> getCategoryByName(String name) {
        logger.debug("Buscando categoria por nome do banco de dados: {}", name);
        return categoryRepository.findByName(name);
    }

    @Cacheable(value = "ticketTypes")
    public List<TicketType> getAllTicketTypes() {
        logger.debug("Buscando todos os tipos de ticket do banco de dados");
        return ticketTypeRepository.findAll();
    }

    @Cacheable(value = "activeTicketTypes")
    public List<TicketType> getActiveTicketTypes() {
        logger.debug("Buscando tipos de ticket ativos do banco de dados");
        return ticketTypeRepository.findByActiveTrue();
    }

    @Cacheable(value = "ticketTypes", key = "#id")
    public Optional<TicketType> getTicketTypeById(Long id) {
        logger.debug("Buscando tipo de ticket por ID do banco de dados: {}", id);
        return ticketTypeRepository.findById(id);
    }

    @Cacheable(value = "ticketTypes", key = "#name")
    public Optional<TicketType> getTicketTypeByName(String name) {
        logger.debug("Buscando tipo de ticket por nome do banco de dados: {}", name);
        return ticketTypeRepository.findByName(name);
    }

    @Cacheable(value = "roles")
    public List<Role> getAllRoles() {
        logger.debug("Buscando todas as roles do banco de dados");
        return roleRepository.findAll();
    }

    @Cacheable(value = "roles", key = "#id")
    public Optional<Role> getRoleById(Long id) {
        logger.debug("Buscando role por ID do banco de dados: {}", id);
        return roleRepository.findById(id);
    }

    @Cacheable(value = "roles", key = "#name")
    public Optional<Role> getRoleByName(String name) {
        logger.debug("Buscando role por nome do banco de dados: {}", name);
        return roleRepository.findByName(name);
    }

    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "activeCategories", allEntries = true)
    })
    public void clearCategoriesCache() {
        logger.debug("Limpando cache de categorias");
    }

    @Caching(evict = {
            @CacheEvict(value = "ticketTypes", allEntries = true),
            @CacheEvict(value = "activeTicketTypes", allEntries = true)
    })
    public void clearTicketTypesCache() {
        logger.debug("Limpando cache de tipos de ticket");
    }

    @CacheEvict(value = "roles", allEntries = true)
    public void clearRolesCache() {
        logger.debug("Limpando cache de roles");
    }

    @Scheduled(fixedRate = 3600000) // Limpa o cache a cada 1 hora
    public void evictAllCaches() {
        logger.info("Limpando todos os caches em schedule programado");
        clearCategoriesCache();
        clearTicketTypesCache();
        clearRolesCache();
    }
}