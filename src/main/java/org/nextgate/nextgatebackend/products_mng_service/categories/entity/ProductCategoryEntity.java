package org.nextgate.nextgatebackend.products_mng_service.categories.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "product_categories")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID categoryId;

    @Column(nullable = false, unique = true)
    private String categoryName;

    private String categoryDescription;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String categoryIconUrl;

    // Hierarchical categories - parent category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ProductCategoryEntity parentCategory;

    private LocalDateTime createdTime;
    private LocalDateTime editedTime;
    private Boolean isActive = true;
    private UUID createdBy;
    private UUID editedBy;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        editedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        editedTime = LocalDateTime.now();
    }
}
