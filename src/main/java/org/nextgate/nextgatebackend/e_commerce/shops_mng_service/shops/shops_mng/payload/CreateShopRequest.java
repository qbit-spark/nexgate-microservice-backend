package org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.payload;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.nextgate.nextgatebackend.e_commerce.shops_mng_service.shops.shops_mng.enums.ShopType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CreateShopRequest {

    // Required fields
    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 100, message = "Shop name must be between 2 and 100 characters")
    private String shopName;

    @NotBlank(message = "Shop description is required")
    @Size(max = 1000, message = "Shop description must not exceed 1000 characters")
    private String shopDescription;

    @URL(message = "Logo URL must be a valid URL")
    @Size(max = 1000, message = "Logo URL must not exceed 1000 characters")
    private String logoUrl;

    @NotNull(message = "Category is required")
    private UUID categoryId;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[0-9]{10,15}$",
            message = "Phone number must be between 10-15 digits and may start with +"
    )
    private String phoneNumber;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;

    @NotBlank(message = "Region is required")
    @Size(min = 2, max = 50, message = "Region must be between 2 and 50 characters")
    private String region;

    // Optional fields
    @Size(max = 50, message = "Tagline must not exceed 50 characters")
    private String tagline;

    private List<@URL @Size(max = 1000) String> shopImages;

    @URL(message = "Banner URL must be a valid URL")
    @Size(max = 1000, message = "Banner URL must not exceed 1000 characters")
    private String bannerUrl;

    private ShopType shopType = ShopType.HYBRID;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @URL(message = "Website URL must be a valid URL")
    @Size(max = 200, message = "Website URL must not exceed 200 characters")
    private String websiteUrl;

    private List<@URL @Size(max = 500) String> socialMediaLinks;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 10, message = "Postal code must not exceed 10 characters")
    private String postalCode;

    @Size(max = 3, message = "Country code must not exceed 3 characters")
    private String countryCode = "TZ";

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Size(max = 300, message = "Location notes must not exceed 300 characters")
    private String locationNotes;

    @Size(max = 50, message = "Business registration number must not exceed 50 characters")
    private String businessRegistrationNumber;

    @Size(max = 50, message = "Tax number must not exceed 50 characters")
    private String taxNumber;

    @Size(max = 50, message = "License number must not exceed 50 characters")
    private String licenseNumber;

    @Size(max = 200, message = "Promotion text must not exceed 200 characters")
    private String promotionText;
}