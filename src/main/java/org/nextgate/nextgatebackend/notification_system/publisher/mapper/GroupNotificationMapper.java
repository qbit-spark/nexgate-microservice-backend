package org.nextgate.nextgatebackend.notification_system.publisher.mapper;

import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupParticipantEntity;
import org.nextgate.nextgatebackend.group_purchase_mng.entity.GroupPurchaseInstanceEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper for Group Purchase notification data
 * Prepares data in the format expected by notification templates
 */
public class GroupNotificationMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
// ==================== GROUP PURCHASE COMPLETED ====================

    /**
     * Map group purchase completed notification
     * Used when group fills up and all participants get their orders
     *
     * Template variables:

     * {{customer.name}}
     * {{customer.email}}
     * {{group.code}}
     * {{group.instanceId}}
     * {{product.name}}
     * {{product.image}}
     * {{shop.name}}
     * {{shop.logo}}
     * {{price.regular}}
     * {{price.group}}
     * {{price.savings}}
     * {{price.savingsPercentage}}
     * {{price.currency}}
     * {{participant.quantity}}
     * {{participant.totalPaid}}
     * {{group.totalParticipants}}
     * {{group.totalSeats}}
     * {{group.completedAt}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapGroupCompleted(
            GroupPurchaseInstanceEntity group,
            GroupParticipantEntity participant) {

        Map<String, Object> data = new HashMap<>();
     // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", participant.getUser().getFirstName());
        customer.put("email", participant.getUser().getEmail());
        data.put("customer", customer);
// Group information
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("code", group.getGroupCode());
        groupInfo.put("instanceId", group.getGroupInstanceId().toString());
        groupInfo.put("totalParticipants", group.getTotalParticipants());
        groupInfo.put("totalSeats", group.getTotalSeats());
        groupInfo.put("completedAt", group.getCompletedAt().format(DATE_FORMATTER));
        data.put("group", groupInfo);
// Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", group.getProductName());
        product.put("image", group.getProductImage());
        data.put("product", product);
// Shop information
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", group.getShop().getShopName());
        shop.put("logo", group.getShop().getLogoUrl());
        data.put("shop", shop);
// Price information
        Map<String, Object> price = new HashMap<>();
        price.put("regular", group.getRegularPrice().toString());
        price.put("group", group.getGroupPrice().toString());
        price.put("savings", group.calculateSavings().toString());
        price.put("savingsPercentage", group.calculateSavingsPercentage().toString());
        price.put("currency", "TZS");
        data.put("price", price);
// Participant information
        Map<String, Object> participantInfo = new HashMap<>();
        participantInfo.put("quantity", participant.getQuantity());
        participantInfo.put("totalPaid", participant.getTotalPaid().toString());
        data.put("participant", participantInfo);
// Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

    /**
     * Map group purchase completed notification for SHOP OWNER
     * <p>
     * Template variables:
     * <p>
     * {{shop.name}}
     * {{group.code}}
     * {{group.instanceId}}
     * {{product.name}}
     * {{product.image}}
     * {{price.group}}
     * {{price.currency}}
     * {{group.totalParticipants}}
     * {{group.totalSeats}}
     * {{group.completedAt}}
     * {{participants}} (list)
     * {{order.totalRevenue}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapGroupCompletedForShopOwner(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants) {

        Map<String, Object> data = new HashMap<>();
// Shop information
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", group.getShop().getShopName());
        data.put("shop", shop);
// Group information
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("code", group.getGroupCode());
        groupInfo.put("instanceId", group.getGroupInstanceId().toString());
        groupInfo.put("totalParticipants", group.getTotalParticipants());
        groupInfo.put("totalSeats", group.getTotalSeats());
        groupInfo.put("completedAt", group.getCompletedAt().format(DATE_FORMATTER));
        data.put("group", groupInfo);
// Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", group.getProductName());
        product.put("image", group.getProductImage());
        data.put("product", product);
// Price information
        Map<String, Object> price = new HashMap<>();
        price.put("group", group.getGroupPrice().toString());
        price.put("currency", "TZS");
        data.put("price", price);
// Participants list (summary)
        List<Map<String, Object>> participantsList = participants.stream()
                .map(p -> {
                    Map<String, Object> pMap = new HashMap<>();
                    pMap.put("name", p.getUser().getFirstName() + " " + p.getUser().getLastName());
                    pMap.put("quantity", p.getQuantity());
                    pMap.put("totalPaid", p.getTotalPaid().toString());
                    return pMap;
                })
                .collect(Collectors.toList());
        data.put("participants", participantsList);
// Calculate total revenue
        BigDecimal totalRevenue = group.getGroupPrice()
                .multiply(BigDecimal.valueOf(group.getSeatsOccupied()));
        Map<String, Object> order = new HashMap<>();
        order.put("totalRevenue", totalRevenue.toString());
        data.put("order", order);
// Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

// ==================== NEW GROUP CREATED ====================

    /**
     * Map new group created notification for SHOP OWNER
     * <p>
     * Template variables:
     * {{shop.name}}
     * {{group.code}}
     * {{group.instanceId}}
     * {{product.name}}
     * {{product.image}}
     * {{price.group}}
     * {{price.regular}}
     * {{price.savings}}
     * {{price.currency}}
     * {{creator.name}}
     * {{group.totalSeats}}
     * {{group.seatsOccupied}}
     * {{group.seatsRemaining}}
     * {{group.progressPercentage}}
     * {{group.expiresAt}}
     * {{group.durationHours}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapNewGroupCreated(
            GroupPurchaseInstanceEntity group,
            AccountEntity creator,
            Integer initialSeats) {

        Map<String, Object> data = new HashMap<>();
// Shop information
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", group.getShop().getShopName());
        data.put("shop", shop);
// Group information
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("code", group.getGroupCode());
        groupInfo.put("instanceId", group.getGroupInstanceId().toString());
        groupInfo.put("totalSeats", group.getTotalSeats());
        groupInfo.put("seatsOccupied", group.getSeatsOccupied());
        groupInfo.put("seatsRemaining", group.getSeatsRemaining());
        groupInfo.put("progressPercentage", calculateProgressPercentage(
                group.getSeatsOccupied(), group.getTotalSeats()));
        groupInfo.put("expiresAt", group.getExpiresAt().format(DATE_FORMATTER));
        groupInfo.put("durationHours", group.getDurationHours());
        data.put("group", groupInfo);
// Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", group.getProductName());
        product.put("image", group.getProductImage());
        data.put("product", product);
// Price information
        Map<String, Object> price = new HashMap<>();
        price.put("regular", group.getRegularPrice().toString());
        price.put("group", group.getGroupPrice().toString());
        price.put("savings", group.calculateSavings().toString());
        price.put("currency", "TZS");
        data.put("price", price);
// Creator information
        Map<String, Object> creatorInfo = new HashMap<>();
        creatorInfo.put("name", creator.getFirstName() + " " + creator.getLastName());
        data.put("creator", creatorInfo);
// Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

// ==================== MEMBER JOINED GROUP ====================

    /**
     * Map member joined notification for SHOP OWNER
     * <p>
     * Template variables:
     * {{shop.name}}
     * {{group.code}}
     * {{product.name}}
     * {{newMember.name}}
     * {{newMember.quantity}}
     * {{group.seatsOccupied}}
     * {{group.totalSeats}}
     * {{group.seatsRemaining}}
     * {{group.progressPercentage}}
     * {{group.expiresAt}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapMemberJoinedForShopOwner(
            GroupPurchaseInstanceEntity group,
            AccountEntity newMember,
            Integer quantity) {

        Map<String, Object> data = new HashMap<>();
// Shop information
        Map<String, Object> shop = new HashMap<>();
        shop.put("name", group.getShop().getShopName());
        data.put("shop", shop);
// Group information
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("code", group.getGroupCode());
        groupInfo.put("seatsOccupied", group.getSeatsOccupied());
        groupInfo.put("totalSeats", group.getTotalSeats());
        groupInfo.put("seatsRemaining", group.getSeatsRemaining());
        groupInfo.put("progressPercentage", calculateProgressPercentage(
                group.getSeatsOccupied(), group.getTotalSeats()));
        groupInfo.put("expiresAt", group.getExpiresAt().format(DATE_FORMATTER));
        data.put("group", groupInfo);
// Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", group.getProductName());
        data.put("product", product);
// New member information
        Map<String, Object> newMemberInfo = new HashMap<>();
        newMemberInfo.put("name", newMember.getFirstName() + " " + newMember.getLastName());
        newMemberInfo.put("quantity", quantity);
        data.put("newMember", newMemberInfo);
// Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

    /**
     * Map member joined notification for EXISTING MEMBERS
     * <p>
     * Template variables:
     * {{customer.name}}
     * {{group.code}}
     * {{product.name}}
     * {{newMember.name}}
     * {{newMember.quantity}}
     * {{group.seatsOccupied}}
     * {{group.totalSeats}}
     * {{group.seatsRemaining}}
     * {{group.progressPercentage}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapMemberJoinedForExistingMembers(
            GroupPurchaseInstanceEntity group,
            AccountEntity existingMember,
            AccountEntity newMember,
            Integer quantity) {

        Map<String, Object> data = new HashMap<>();
// Customer (existing member) information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", existingMember.getFirstName());
        data.put("customer", customer);
// Group information
        Map<String, Object> groupInfo = new HashMap<>();
        groupInfo.put("code", group.getGroupCode());
        groupInfo.put("seatsOccupied", group.getSeatsOccupied());
        groupInfo.put("totalSeats", group.getTotalSeats());
        groupInfo.put("seatsRemaining", group.getSeatsRemaining());
        groupInfo.put("progressPercentage", calculateProgressPercentage(
                group.getSeatsOccupied(), group.getTotalSeats()));
        data.put("group", groupInfo);
// Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", group.getProductName());
        data.put("product", product);
// New member information
        Map<String, Object> newMemberInfo = new HashMap<>();
        newMemberInfo.put("name", newMember.getFirstName());
        newMemberInfo.put("quantity", quantity);
        data.put("newMember", newMemberInfo);
// Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

// ==================== SEATS TRANSFERRED ====================

    /**
     * Map seats transferred notification
     * <p>
     * Template variables:
     * {{customer.name}}
     * {{transfer.quantity}}
     * {{source.groupCode}}
     * {{target.groupCode}}
     * {{product.name}}
     * {{target.seatsOccupied}}
     * {{target.totalSeats}}
     * {{target.progressPercentage}}
     * {{timestamp}}
     */
    public static Map<String, Object> mapSeatsTransferred(
            GroupPurchaseInstanceEntity sourceGroup,
            GroupPurchaseInstanceEntity targetGroup,
            AccountEntity user,
            Integer quantity) {

        Map<String, Object> data = new HashMap<>();
        // Customer information
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", user.getFirstName());
        data.put("customer", customer);
        // Transfer information
        Map<String, Object> transfer = new HashMap<>();
        transfer.put("quantity", quantity);
        data.put("transfer", transfer);
        // Source group information
        Map<String, Object> source = new HashMap<>();
        source.put("groupCode", sourceGroup.getGroupCode());
        data.put("source", source);
        // Target group information
        Map<String, Object> target = new HashMap<>();
        target.put("groupCode", targetGroup.getGroupCode());
        target.put("seatsOccupied", targetGroup.getSeatsOccupied());
        target.put("totalSeats", targetGroup.getTotalSeats());
        target.put("progressPercentage", calculateProgressPercentage(
                targetGroup.getSeatsOccupied(), targetGroup.getTotalSeats()));
        data.put("target", target);
        // Product information
        Map<String, Object> product = new HashMap<>();
        product.put("name", targetGroup.getProductName());
        data.put("product", product);
        // Timestamp
        data.put("timestamp", LocalDateTime.now().format(DATE_FORMATTER));
        return data;
    }

    // ==================== HELPER METHODS ====================
    private static String calculateProgressPercentage(int occupied, int total) {
        if (total == 0) return "0.00";
        return String.format("%.2f", (occupied * 100.0) / total);
    }


    /**
     * Map data for group failure notification to participants
     */
    public static Map<String, Object> mapGroupFailed(
            GroupPurchaseInstanceEntity group,
            GroupParticipantEntity participant) {

        Map<String, Object> data = new HashMap<>();

        // Group info
        data.put("groupCode", group.getGroupCode());
        data.put("productName", group.getProductName());
        data.put("productImage", group.getProductImage());
        data.put("groupPrice", group.getGroupPrice());

        // Participant info
        data.put("quantity", participant.getQuantity());
        data.put("amountPaid", participant.getTotalPaid());
        data.put("refundStatus", "PROCESSING");

        // Group stats
        data.put("seatsFilled", group.getSeatsOccupied());
        data.put("totalSeats", group.getTotalSeats());
        data.put("seatsUnfilled", group.getSeatsRemaining());

        // Timing
        data.put("expiresAt", group.getExpiresAt());
        data.put("failedAt", LocalDateTime.now());

        return data;
    }

    /**
     * Map data for group failure notification to shop owner
     */
    public static Map<String, Object> mapGroupFailedForShopOwner(
            GroupPurchaseInstanceEntity group,
            List<GroupParticipantEntity> participants) {

        Map<String, Object> data = new HashMap<>();

        // Group info
        data.put("groupCode", group.getGroupCode());
        data.put("productName", group.getProductName());
        data.put("shopName", group.getShop().getShopName());

        // Stats
        data.put("totalParticipants", participants.size());
        data.put("seatsFilled", group.getSeatsOccupied());
        data.put("totalSeats", group.getTotalSeats());
        data.put("seatsUnfilled", group.getSeatsRemaining());

        // Financial
        BigDecimal totalRevenueLost = participants.stream()
                .map(GroupParticipantEntity::getTotalPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalRevenueLost", totalRevenueLost);

        // Timing
        data.put("createdAt", group.getCreatedAt());
        data.put("expiresAt", group.getExpiresAt());
        data.put("failedAt", LocalDateTime.now());

        return data;
    }
}