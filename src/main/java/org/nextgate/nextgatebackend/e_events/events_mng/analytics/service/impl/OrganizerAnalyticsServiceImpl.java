package org.nextgate.nextgatebackend.e_events.events_mng.analytics.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.analytics.payload.*;
import org.nextgate.nextgatebackend.e_events.events_mng.analytics.service.OrganizerAnalyticsService;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.EventBookingOrderRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.enums.EventStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.utils.EventCapacityHelper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizerAnalyticsServiceImpl implements OrganizerAnalyticsService {

    private final EventsRepo eventsRepo;
    private final EventBookingOrderRepo bookingOrderRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional(readOnly = true)
    public CollectionSummaryResponse getCollectionSummary() throws ItemNotFoundException {

        AccountEntity organizer = getAuthenticatedAccount();
        List<EventEntity> allEvents = eventsRepo.findByOrganizerAndIsDeletedFalse(organizer);

        Map<EventStatus, Long> eventsByStatus = allEvents.stream()
                .collect(Collectors.groupingBy(EventEntity::getStatus, Collectors.counting()));

        List<EventBookingOrderEntity> allBookings = allEvents.stream()
                .flatMap(event -> bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED).stream())
                .toList();

        int totalTickets = allBookings.stream()
                .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                .sum();

        BigDecimal totalRevenue = allBookings.stream()
                .map(EventBookingOrderEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inEscrow = calculateInEscrow(allEvents, allBookings);
        BigDecimal released = calculateReleased(allEvents, allBookings);
        BigDecimal refunded = BigDecimal.ZERO;

        CollectionSummaryResponse.EventMetrics eventMetrics = CollectionSummaryResponse.EventMetrics.builder()
                .totalEvents(allEvents.size())
                .upcomingEvents(eventsByStatus.getOrDefault(EventStatus.PUBLISHED, 0L).intValue())
                .ongoingEvents(eventsByStatus.getOrDefault(EventStatus.HAPPENING, 0L).intValue())
                .completedEvents(eventsByStatus.getOrDefault(EventStatus.COMPLETED, 0L).intValue())
                .cancelledEvents(eventsByStatus.getOrDefault(EventStatus.CANCELLED, 0L).intValue())
                .build();

        CollectionSummaryResponse.CollectionMetrics collectionMetrics = CollectionSummaryResponse.CollectionMetrics.builder()
                .totalTicketsSold(totalTickets)
                .totalRevenue(totalRevenue)
                .inEscrow(inEscrow)
                .released(released)
                .refunded(refunded)
                .pendingRefunds(BigDecimal.ZERO)
                .build();

        CollectionSummaryResponse.TopPerformer topEvent = findTopPerformer(allEvents);

        return CollectionSummaryResponse.builder()
                .eventMetrics(eventMetrics)
                .collectionMetrics(collectionMetrics)
                .topEvent(topEvent)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EventRevenueResponse getEventRevenue(String status, LocalDate startDate, LocalDate endDate, Pageable pageable)
            throws ItemNotFoundException {

        AccountEntity organizer = getAuthenticatedAccount();
        List<EventEntity> allEvents = eventsRepo.findByOrganizerAndIsDeletedFalse(organizer);

        List<EventEntity> filteredEvents = allEvents.stream()
                .filter(event -> status == null || event.getStatus().name().equals(status))
                .filter(event -> startDate == null || !event.getStartDateTime().toLocalDate().isBefore(startDate))
                .filter(event -> endDate == null || !event.getStartDateTime().toLocalDate().isAfter(endDate))
                .sorted(Comparator.comparing(EventEntity::getStartDateTime).reversed())
                .toList();

        Page<EventEntity> pagedEvents = paginateList(filteredEvents, pageable);

        List<EventRevenueResponse.EventRevenue> eventRevenues = pagedEvents.getContent().stream()
                .map(this::mapToEventRevenue)
                .toList();

        EventRevenueResponse.PaginationInfo paginationInfo = EventRevenueResponse.PaginationInfo.builder()
                .currentPage(pagedEvents.getNumber())
                .pageSize(pagedEvents.getSize())
                .totalPages(pagedEvents.getTotalPages())
                .totalElements(pagedEvents.getTotalElements())
                .hasNext(pagedEvents.hasNext())
                .hasPrevious(pagedEvents.hasPrevious())
                .build();

        return EventRevenueResponse.builder()
                .events(eventRevenues)
                .pagination(paginationInfo)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EventPerformanceResponse getEventPerformance(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));

        AccountEntity organizer = getAuthenticatedAccount();
        if (!event.getOrganizer().getId().equals(organizer.getId())) {
            throw new AccessDeniedException("Only event organizer can view analytics");
        }

        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);

        int totalTickets = bookings.stream()
                .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                .sum();

        BigDecimal totalRevenue = bookings.stream()
                .map(EventBookingOrderEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inEscrow = isEventCompleted(event) ? BigDecimal.ZERO : totalRevenue;
        BigDecimal released = isEventCompleted(event) ? totalRevenue : BigDecimal.ZERO;

        BigDecimal avgPrice = totalTickets > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        EventPerformanceResponse.FinancialMetrics financials = EventPerformanceResponse.FinancialMetrics.builder()
                .totalRevenue(totalRevenue)
                .inEscrow(inEscrow)
                .released(released)
                .refunded(BigDecimal.ZERO)
                .averageTicketPrice(avgPrice)
                .build();

        CapacityMetrics capacityMetrics = EventCapacityHelper.calculateCapacityMetrics(event.getTickets());

        EventPerformanceResponse.TicketMetrics ticketMetrics = EventPerformanceResponse.TicketMetrics.builder()
                .hasUnlimitedTickets(capacityMetrics.getHasUnlimitedTickets())
                .limitedCapacity(capacityMetrics.getLimitedCapacity())
                .limitedSold(capacityMetrics.getLimitedSold())
                .limitedRemaining(capacityMetrics.getLimitedRemaining())
                .unlimitedSold(capacityMetrics.getUnlimitedSold())
                .totalSold(capacityMetrics.getTotalSold())
                .displayCapacity(capacityMetrics.getDisplayCapacity())
                .sellOutPercentage(capacityMetrics.getSellOutPercentage())
                .build();

        long checkedIn = bookings.stream()
                .mapToLong(EventBookingOrderEntity::getCheckedInCount)
                .sum();

        double attendanceRate = totalTickets > 0 ? (checkedIn * 100.0) / totalTickets : 0.0;

        EventPerformanceResponse.AttendanceMetrics attendanceMetrics = EventPerformanceResponse.AttendanceMetrics.builder()
                .totalTickets(totalTickets)
                .checkedIn((int) checkedIn)
                .noShows(totalTickets - (int) checkedIn)
                .attendanceRate(attendanceRate)
                .build();

        LocalDateTime firstSaleAt = bookings.stream()
                .map(EventBookingOrderEntity::getBookedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        EventPerformanceResponse.EventTimeline timeline = EventPerformanceResponse.EventTimeline.builder()
                .createdAt(event.getCreatedAt().toLocalDateTime())
                .publishedAt(event.getPublishedAt())
                .firstSaleAt(firstSaleAt)
                .eventDate(event.getStartDateTime().toLocalDateTime())
                .completedAt(event.getEndDateTime().toLocalDateTime())
                .build();

        return EventPerformanceResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventDate(event.getStartDateTime().toLocalDateTime())
                .status(event.getStatus().name())
                .financials(financials)
                .ticketMetrics(ticketMetrics)
                .attendanceMetrics(attendanceMetrics)
                .timeline(timeline)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueTrendResponse getRevenueTrends(String period, Integer year) throws ItemNotFoundException {

        AccountEntity organizer = getAuthenticatedAccount();
        List<EventEntity> allEvents = eventsRepo.findByOrganizerAndIsDeletedFalse(organizer);

        int targetYear = year != null ? year : LocalDate.now().getYear();

        List<EventEntity> yearEvents = allEvents.stream()
                .filter(event -> event.getStartDateTime().getYear() == targetYear)
                .toList();

        List<RevenueTrendResponse.PeriodData> trends;

        if ("MONTHLY".equalsIgnoreCase(period)) {
            trends = calculateMonthlyTrends(yearEvents, targetYear);
        } else if ("YEARLY".equalsIgnoreCase(period)) {
            trends = calculateYearlyTrends(allEvents);
        } else {
            trends = calculateMonthlyTrends(yearEvents, targetYear);
        }

        return RevenueTrendResponse.builder()
                .period(period != null ? period.toUpperCase() : "MONTHLY")
                .totalEvents(yearEvents.size())
                .trends(trends)
                .build();
    }

    private List<RevenueTrendResponse.PeriodData> calculateMonthlyTrends(List<EventEntity> events, int year) {
        Map<Integer, List<EventEntity>> eventsByMonth = events.stream()
                .collect(Collectors.groupingBy(event -> event.getStartDateTime().getMonthValue()));

        List<RevenueTrendResponse.PeriodData> trends = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            List<EventEntity> monthEvents = eventsByMonth.getOrDefault(month, Collections.emptyList());

            List<EventBookingOrderEntity> monthBookings = monthEvents.stream()
                    .flatMap(event -> bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED).stream())
                    .toList();

            int ticketsSold = monthBookings.stream()
                    .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                    .sum();

            BigDecimal revenue = monthBookings.stream()
                    .map(EventBookingOrderEntity::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal inEscrow = calculateInEscrow(monthEvents, monthBookings);
            BigDecimal released = calculateReleased(monthEvents, monthBookings);

            double avgAttendance = monthEvents.stream()
                    .mapToDouble(this::calculateEventAttendanceRate)
                    .average()
                    .orElse(0.0);

            double avgSellOut = monthEvents.stream()
                    .mapToDouble(this::calculateSellOutRate)
                    .average()
                    .orElse(0.0);

            String monthName = YearMonth.of(year, month).getMonth().toString();

            trends.add(RevenueTrendResponse.PeriodData.builder()
                    .label(monthName.substring(0, 3))
                    .year(year)
                    .month(month)
                    .eventsCount(monthEvents.size())
                    .ticketsSold(ticketsSold)
                    .revenue(revenue)
                    .inEscrow(inEscrow)
                    .released(released)
                    .averageAttendanceRate(avgAttendance)
                    .averageSellOutRate(avgSellOut)
                    .build());
        }

        return trends;
    }

    private List<RevenueTrendResponse.PeriodData> calculateYearlyTrends(List<EventEntity> allEvents) {
        Map<Integer, List<EventEntity>> eventsByYear = allEvents.stream()
                .collect(Collectors.groupingBy(event -> event.getStartDateTime().getYear()));

        return eventsByYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    int year = entry.getKey();
                    List<EventEntity> yearEvents = entry.getValue();

                    List<EventBookingOrderEntity> yearBookings = yearEvents.stream()
                            .flatMap(event -> bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED).stream())
                            .toList();

                    int ticketsSold = yearBookings.stream()
                            .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                            .sum();

                    BigDecimal revenue = yearBookings.stream()
                            .map(EventBookingOrderEntity::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal inEscrow = calculateInEscrow(yearEvents, yearBookings);
                    BigDecimal released = calculateReleased(yearEvents, yearBookings);

                    double avgAttendance = yearEvents.stream()
                            .mapToDouble(this::calculateEventAttendanceRate)
                            .average()
                            .orElse(0.0);

                    double avgSellOut = yearEvents.stream()
                            .mapToDouble(this::calculateSellOutRate)
                            .average()
                            .orElse(0.0);

                    return RevenueTrendResponse.PeriodData.builder()
                            .label(String.valueOf(year))
                            .year(year)
                            .eventsCount(yearEvents.size())
                            .ticketsSold(ticketsSold)
                            .revenue(revenue)
                            .inEscrow(inEscrow)
                            .released(released)
                            .averageAttendanceRate(avgAttendance)
                            .averageSellOutRate(avgSellOut)
                            .build();
                })
                .toList();
    }

    private BigDecimal calculateInEscrow(List<EventEntity> events, List<EventBookingOrderEntity> bookings) {
        Map<UUID, BigDecimal> revenueByEvent = bookings.stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getEvent().getId(),
                        Collectors.reducing(BigDecimal.ZERO, EventBookingOrderEntity::getTotal, BigDecimal::add)
                ));

        return events.stream()
                .filter(event -> !isEventCompleted(event))
                .map(event -> revenueByEvent.getOrDefault(event.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateReleased(List<EventEntity> events, List<EventBookingOrderEntity> bookings) {
        Map<UUID, BigDecimal> revenueByEvent = bookings.stream()
                .collect(Collectors.groupingBy(
                        booking -> booking.getEvent().getId(),
                        Collectors.reducing(BigDecimal.ZERO, EventBookingOrderEntity::getTotal, BigDecimal::add)
                ));

        return events.stream()
                .filter(this::isEventCompleted)
                .map(event -> revenueByEvent.getOrDefault(event.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isEventCompleted(EventEntity event) {
        return event.getStatus() == EventStatus.COMPLETED;
    }

    private CollectionSummaryResponse.TopPerformer findTopPerformer(List<EventEntity> events) {
        return events.stream()
                .map(event -> {
                    List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);

                    int tickets = bookings.stream()
                            .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                            .sum();

                    BigDecimal revenue = bookings.stream()
                            .map(EventBookingOrderEntity::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double attendance = calculateEventAttendanceRate(event);

                    return CollectionSummaryResponse.TopPerformer.builder()
                            .eventId(event.getId())
                            .eventTitle(event.getTitle())
                            .revenue(revenue)
                            .ticketsSold(tickets)
                            .attendanceRate(attendance)
                            .build();
                })
                .max(Comparator.comparing(CollectionSummaryResponse.TopPerformer::getRevenue))
                .orElse(null);
    }

    private EventRevenueResponse.EventRevenue mapToEventRevenue(EventEntity event) {
        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);

        int tickets = bookings.stream()
                .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                .sum();

        BigDecimal revenue = bookings.stream()
                .map(EventBookingOrderEntity::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal inEscrow = isEventCompleted(event) ? BigDecimal.ZERO : revenue;
        BigDecimal released = isEventCompleted(event) ? revenue : BigDecimal.ZERO;

        double attendance = calculateEventAttendanceRate(event);

        CapacityMetrics capacityMetrics = EventCapacityHelper.calculateCapacityMetrics(event.getTickets());
        Integer displayCapacity = capacityMetrics.getDisplayCapacity();
        Double sellOutPercentage = capacityMetrics.getSellOutPercentage();

        return EventRevenueResponse.EventRevenue.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventDate(event.getStartDateTime().toLocalDateTime())
                .status(event.getStatus().name())
                .ticketsSold(tickets)
                .totalRevenue(revenue)
                .inEscrow(inEscrow)
                .released(released)
                .refunded(BigDecimal.ZERO)
                .attendanceRate(attendance)
                .totalCapacity(displayCapacity)
                .sellOutPercentage(sellOutPercentage)
                .build();
    }

    private double calculateEventAttendanceRate(EventEntity event) {
        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);

        int totalTickets = bookings.stream()
                .mapToInt(EventBookingOrderEntity::getTotalTicketCount)
                .sum();

        if (totalTickets == 0) return 0.0;

        long checkedIn = bookings.stream()
                .mapToLong(EventBookingOrderEntity::getCheckedInCount)
                .sum();

        return (checkedIn * 100.0) / totalTickets;
    }

    private double calculateSellOutRate(EventEntity event) {
        CapacityMetrics capacityMetrics = EventCapacityHelper.calculateCapacityMetrics(event.getTickets());
        return capacityMetrics.getSellOutPercentage();
    }

    private <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        if (start > list.size()) return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    private AccountEntity getAuthenticatedAccount() throws ItemNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return accountRepo.findByUserName(userDetails.getUsername())
                    .orElseThrow(() -> new ItemNotFoundException("User not found"));
        }
        throw new ItemNotFoundException("User not authenticated");
    }
}