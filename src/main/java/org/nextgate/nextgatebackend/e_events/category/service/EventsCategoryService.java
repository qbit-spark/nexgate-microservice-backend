package org.nextgate.nextgatebackend.e_events.category.service;

import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;
import org.nextgate.nextgatebackend.e_events.category.payloads.CreateEventCategoryRequest;
import org.nextgate.nextgatebackend.e_events.category.payloads.UpdateEventCategoryRequest;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemReadyExistException;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface EventsCategoryService {
    EventsCategoryEntity createCategory(CreateEventCategoryRequest createEventCategoryRequest) throws AccessDeniedException, ItemNotFoundException, ItemReadyExistException;

    EventsCategoryEntity updateCategory(UUID categoryId, UpdateEventCategoryRequest updateRequest) throws AccessDeniedException, ItemNotFoundException, ItemReadyExistException;

    public EventsCategoryEntity getCategoryById(UUID categoryId) throws ItemNotFoundException;

    EventsCategoryEntity getCategoryBySlug(String slug) throws ItemNotFoundException;

    List<EventsCategoryEntity> getAllCategories();

    Page<EventsCategoryEntity> getPaginatedCategories(int page, int size);

    List<EventsCategoryEntity> seedCategories() throws ItemNotFoundException, AccessDeniedException;
}
