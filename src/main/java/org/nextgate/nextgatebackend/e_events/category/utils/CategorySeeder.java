package org.nextgate.nextgatebackend.e_events.category.utils;

import org.nextgate.nextgatebackend.e_events.category.entity.EventsCategoryEntity;

import java.util.List;

public class CategorySeeder {

    public static List<EventsCategoryEntity> getDefaultCategories() {
        return List.of(
                // 1. Music & Concerts
                EventsCategoryEntity.builder()
                        .name("Music & Concerts")
                        .slug("music-concerts")
                        .description("Live music performances, concerts, festivals, and DJ events")
                        .iconUrl("")
                        .colorCode("#E91E63")
                        .isActive(true)
                        .isFeatured(true)
                        .eventCount(0L)
                        .build(),

                // 2. Sports & Fitness
                EventsCategoryEntity.builder()
                        .name("Sports & Fitness")
                        .slug("sports-fitness")
                        .description("Yoga, gym classes, marathons, tournaments, and outdoor activities")
                        .iconUrl("")
                        .colorCode("#4CAF50")
                        .isActive(true)
                        .isFeatured(true)
                        .eventCount(0L)
                        .build(),

                // 3. Business & Networking
                EventsCategoryEntity.builder()
                        .name("Business & Networking")
                        .slug("business-networking")
                        .description("Professional meetups, conferences, workshops, and networking events")
                        .iconUrl("")
                        .colorCode("#2196F3")
                        .isActive(true)
                        .isFeatured(true)
                        .eventCount(0L)
                        .build(),

                // 4. Food & Drink
                EventsCategoryEntity.builder()
                        .name("Food & Drink")
                        .slug("food-drink")
                        .description("Food festivals, cooking classes, wine tastings, and dining experiences")
                        .iconUrl("")
                        .colorCode("#FF9800")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build(),

                // 5. Arts & Culture
                EventsCategoryEntity.builder()
                        .name("Arts & Culture")
                        .slug("arts-culture")
                        .description("Art exhibitions, theater, dance, museums, and cultural events")
                        .iconUrl("")
                        .colorCode("#9C27B0")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build(),

                // 6. Education & Learning
                EventsCategoryEntity.builder()
                        .name("Education & Learning")
                        .slug("education-learning")
                        .description("Workshops, seminars, courses, bootcamps, and training sessions")
                        .iconUrl("")
                        .colorCode("#00BCD4")
                        .isActive(true)
                        .isFeatured(true)
                        .eventCount(0L)
                        .build(),

                // 7. Social & Community
                EventsCategoryEntity.builder()
                        .name("Social & Community")
                        .slug("social-community")
                        .description("Parties, meetups, social clubs, game nights, and community events")
                        .iconUrl("")
                        .colorCode("#FF5722")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build(),

                // 8. Technology & Innovation
                EventsCategoryEntity.builder()
                        .name("Technology & Innovation")
                        .slug("technology-innovation")
                        .description("Tech talks, hackathons, product launches, and startup events")
                        .iconUrl("")
                        .colorCode("#607D8B")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build(),

                // 9. Wellness & Spirituality
                EventsCategoryEntity.builder()
                        .name("Wellness & Spirituality")
                        .slug("wellness-spirituality")
                        .description("Meditation, yoga retreats, healing workshops, and mindfulness events")
                        .iconUrl("")
                        .colorCode("#8BC34A")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build(),

                // 10. Entertainment
                EventsCategoryEntity.builder()
                        .name("Entertainment")
                        .slug("entertainment")
                        .description("Comedy shows, movie screenings, gaming, and entertainment events")
                        .iconUrl("")
                        .colorCode("#F44336")
                        .isActive(true)
                        .isFeatured(false)
                        .eventCount(0L)
                        .build()
        );
    }
}