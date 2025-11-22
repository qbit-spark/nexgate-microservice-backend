package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.categories.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.utils.StringListJsonConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "shop_category")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShopCategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID categoryId;
    private String categoryName;
    private String categoryDescription;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String categoryIconUrl;

    private LocalDateTime createdTime;
    private LocalDateTime editedTime;
    private Boolean isActive;
    private UUID createdBy;
    private UUID editedBy;
}
