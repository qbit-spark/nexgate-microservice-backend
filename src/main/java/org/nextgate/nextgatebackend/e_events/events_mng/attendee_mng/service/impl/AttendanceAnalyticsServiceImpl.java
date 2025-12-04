package org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.authentication_service.entity.AccountEntity;
import org.nextgate.nextgatebackend.authentication_service.repo.AccountRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums.AbsenteeCategory;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums.AttendanceStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.enums.DayStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.payload.*;
import org.nextgate.nextgatebackend.e_events.events_mng.attendee_mng.service.AttendanceAnalyticsService;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.enums.BookingStatus;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.repo.EventBookingOrderRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventDayEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.entity.EventEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.events_core.repo.EventsRepo;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.entity.TicketEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.ticket_mng.repo.TicketRepo;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceAnalyticsServiceImpl implements AttendanceAnalyticsService {

    private final EventsRepo eventsRepo;
    private final EventBookingOrderRepo bookingOrderRepo;
    private final TicketRepo ticketRepo;
    private final AccountRepo accountRepo;

    @Override
    @Transactional(readOnly = true)
    public AttendanceStatsResponse getAttendanceStats(UUID eventId)
            throws ItemNotFoundException, AccessDeniedException {

        EventEntity event = fetchAndValidateEvent(eventId);
        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);
        List<EventBookingOrderEntity.BookedTicket> allTickets = bookings.stream()
                .flatMap(booking -> booking.getBookedTickets().stream())
                .toList();

        List<AttendanceStatsResponse.EventDaySchedule> eventSchedule = buildEventSchedule(event);

        int totalTickets = allTickets.size();
        long uniqueCheckedIn = allTickets.stream().filter(EventBookingOrderEntity.BookedTicket::hasAnyCheckIn).count();
        int totalAbsent = totalTickets - (int) uniqueCheckedIn;
        double attendanceRate = totalTickets > 0 ? (uniqueCheckedIn * 100.0) / totalTickets : 0.0;

        List<AttendanceStatsResponse.DayStats> dayStats = calculateDayStats(event, allTickets);
        List<AttendanceStatsResponse.TicketTypeStats> ticketTypeStats = calculateTicketTypeStats(event, allTickets);

        AttendanceStatsResponse.OverallStats stats = AttendanceStatsResponse.OverallStats.builder()
                .totalTickets(totalTickets)
                .totalCheckedIn((int) uniqueCheckedIn)
                .totalAbsent(totalAbsent)
                .attendanceRate(attendanceRate)
                .byDay(dayStats)
                .build();

        return AttendanceStatsResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalDays(eventSchedule.size())
                .eventSchedule(eventSchedule)
                .overallStats(stats)
                .byTicketType(ticketTypeStats)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendeeListResponse getAttendees(UUID eventId, Integer dayNumber, UUID ticketTypeId, String search, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        EventEntity event = fetchAndValidateEvent(eventId);
        validateDayNumber(event, dayNumber);

        String dayName = getDayNameFromNumber(event, dayNumber != null ? dayNumber : 1);

        if (ticketTypeId != null) {
            validateTicketBelongsToEvent(event, ticketTypeId);
        }

        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);
        List<EventBookingOrderEntity.BookedTicket> allTickets = bookings.stream()
                .flatMap(booking -> booking.getBookedTickets().stream())
                .toList();

        List<EventBookingOrderEntity.BookedTicket> filteredTickets = allTickets.stream()
                .filter(ticket -> ticket.isCheckedInForDay(dayName))
                .filter(ticket -> ticketTypeId == null || ticket.getTicketTypeId().equals(ticketTypeId))
                .toList();

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            filteredTickets = filteredTickets.stream()
                    .filter(ticket -> ticket.getAttendeeName().toLowerCase().contains(searchLower) ||
                            ticket.getAttendeeEmail().toLowerCase().contains(searchLower))
                    .toList();
        }

        Page<EventBookingOrderEntity.BookedTicket> pagedTickets = paginateList(filteredTickets, pageable);

        List<AttendeeListResponse.AttendeeInfo> attendeeInfos = pagedTickets.getContent().stream()
                .map(ticket -> mapToAttendeeInfo(ticket, dayName))
                .toList();

        int totalForType = ticketTypeId != null
                ? (int) allTickets.stream().filter(t -> t.getTicketTypeId().equals(ticketTypeId)).count()
                : allTickets.size();

        AttendeeListResponse.DaySummary summary = AttendeeListResponse.DaySummary.builder()
                .totalTicketsForType(totalForType)
                .checkedInThisDay(filteredTickets.size())
                .build();

        TicketEntity ticketEntity = ticketTypeId != null ? ticketRepo.findById(ticketTypeId).orElse(null) : null;

        return AttendeeListResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .dayNumber(dayNumber != null ? dayNumber : 1)
                .dayName(dayName)
                .dayDate(getDayDateFromNumber(event, dayNumber != null ? dayNumber : 1))
                .ticketTypeId(ticketTypeId)
                .ticketTypeName(ticketEntity != null ? ticketEntity.getName() : null)
                .summary(summary)
                .attendees(attendeeInfos)
                .pagination(buildPaginationInfo(pagedTickets))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AbsenteeListResponse getAbsentees(UUID eventId, Integer dayNumber, UUID ticketTypeId,
                                             AbsenteeCategory category, String search, Pageable pageable)
            throws ItemNotFoundException, AccessDeniedException {

        EventEntity event = fetchAndValidateEvent(eventId);
        validateDayNumber(event, dayNumber);

        String dayName = getDayNameFromNumber(event, dayNumber != null ? dayNumber : 1);

        if (ticketTypeId != null) {
            validateTicketBelongsToEvent(event, ticketTypeId);
        }

        List<EventBookingOrderEntity> bookings = bookingOrderRepo.findByEventAndStatus(event, BookingStatus.CONFIRMED);
        List<EventBookingOrderEntity.BookedTicket> allTickets = bookings.stream()
                .flatMap(booking -> booking.getBookedTickets().stream())
                .toList();

        List<EventBookingOrderEntity.BookedTicket> absentees = allTickets.stream()
                .filter(ticket -> !ticket.isCheckedInForDay(dayName))
                .filter(ticket -> ticketTypeId == null || ticket.getTicketTypeId().equals(ticketTypeId))
                .toList();

        if (category != null && category != AbsenteeCategory.ALL) {
            absentees = filterByCategory(absentees, dayName, category);
        }

        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            absentees = absentees.stream()
                    .filter(ticket -> ticket.getAttendeeName().toLowerCase().contains(searchLower) ||
                            ticket.getAttendeeEmail().toLowerCase().contains(searchLower))
                    .toList();
        }

        List<EventBookingOrderEntity.BookedTicket> ticketsForType = ticketTypeId != null
                ? allTickets.stream().filter(t -> t.getTicketTypeId().equals(ticketTypeId)).toList()
                : allTickets;

        int fullNoShow = (int) ticketsForType.stream().filter(ticket -> !ticket.hasAnyCheckIn()).count();
        int specificDayOnly = absentees.size() - fullNoShow;

        Page<EventBookingOrderEntity.BookedTicket> pagedAbsentees = paginateList(absentees, pageable);

        List<AbsenteeListResponse.AbsenteeInfo> absenteeInfos = pagedAbsentees.getContent().stream()
                .map(ticket -> mapToAbsenteeInfo(ticket, event, dayNumber != null ? dayNumber : 1))
                .toList();

        AbsenteeListResponse.AbsenteeBreakdown breakdown = AbsenteeListResponse.AbsenteeBreakdown.builder()
                .fullNoShow(fullNoShow)
                .specificDayOnly(Math.max(0, specificDayOnly))
                .build();

        AbsenteeListResponse.AbsenteeSummary summary = AbsenteeListResponse.AbsenteeSummary.builder()
                .totalTicketsForType(ticketsForType.size())
                .absentThisDay(absentees.size())
                .absenteeRate(ticketsForType.size() > 0 ? (absentees.size() * 100.0) / ticketsForType.size() : 0.0)
                .breakdown(breakdown)
                .build();

        TicketEntity ticketEntity = ticketTypeId != null ? ticketRepo.findById(ticketTypeId).orElse(null) : null;

        return AbsenteeListResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .dayNumber(dayNumber != null ? dayNumber : 1)
                .dayName(dayName)
                .dayDate(getDayDateFromNumber(event, dayNumber != null ? dayNumber : 1))
                .ticketTypeId(ticketTypeId)
                .ticketTypeName(ticketEntity != null ? ticketEntity.getName() : null)
                .summary(summary)
                .absentees(absenteeInfos)
                .pagination(buildPaginationInfo(pagedAbsentees))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AttendeeDetailResponse getAttendeeDetail(UUID eventId, UUID ticketInstanceId)
            throws ItemNotFoundException, AccessDeniedException {

        EventEntity event = fetchAndValidateEvent(eventId);

        EventBookingOrderEntity booking = bookingOrderRepo.findAll().stream()
                .filter(b -> b.getEvent().getId().equals(eventId))
                .filter(b -> b.getBookedTickets().stream().anyMatch(t -> t.getTicketInstanceId().equals(ticketInstanceId)))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Ticket not found"));

        EventBookingOrderEntity.BookedTicket ticket = booking.getBookedTickets().stream()
                .filter(t -> t.getTicketInstanceId().equals(ticketInstanceId))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException("Ticket not found"));

        List<AttendeeDetailResponse.DayCheckInInfo> checkInsByDay = buildCheckInHistory(event, ticket);

        int daysAttended = (int) checkInsByDay.stream().filter(day -> "CHECKED_IN".equals(day.getStatus())).count();
        int totalDays = checkInsByDay.size();

        return AttendeeDetailResponse.builder()
                .ticketInstanceId(ticket.getTicketInstanceId())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .attendeePhone(ticket.getAttendeePhone())
                .ticketType(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .bookingReference(booking.getBookingReference())
                .pricePaid(ticket.getPrice())
                .overallStatus(determineOverallStatus(daysAttended, totalDays))
                .daysAttended(daysAttended)
                .daysTotal(totalDays)
                .checkInsByDay(checkInsByDay)
                .build();
    }

    private EventEntity fetchAndValidateEvent(UUID eventId) throws ItemNotFoundException, AccessDeniedException {
        EventEntity event = eventsRepo.findByIdAndIsDeletedFalse(eventId)
                .orElseThrow(() -> new ItemNotFoundException("Event not found"));
        validateEventOwnership(event, getAuthenticatedAccount());
        return event;
    }

    private void validateDayNumber(EventEntity event, Integer dayNumber) throws ItemNotFoundException {
        if (dayNumber == null) {
            if (event.getDays() != null && event.getDays().size() > 1) {
                throw new ItemNotFoundException("Day number required for multi-day events");
            }
            return;
        }

        int totalDays = event.getDays() != null && !event.getDays().isEmpty() ? event.getDays().size() : 1;

        if (dayNumber < 1 || dayNumber > totalDays) {
            throw new ItemNotFoundException(
                    String.format("Invalid day %d. Valid: 1-%d", dayNumber, totalDays));
        }
    }

    private void validateTicketBelongsToEvent(EventEntity event, UUID ticketTypeId) throws ItemNotFoundException {
        TicketEntity ticket = ticketRepo.findById(ticketTypeId)
                .orElseThrow(() -> new ItemNotFoundException("Ticket type not found"));

        if (!ticket.getEvent().getId().equals(event.getId())) {
            throw new ItemNotFoundException("Ticket type does not belong to this event");
        }
    }

    private String getDayNameFromNumber(EventEntity event, Integer dayNumber) {
        if (event.getDays() != null && !event.getDays().isEmpty()) {
            return event.getDays().get(dayNumber - 1).getDescription();
        }
        return "Day 1";
    }

    private LocalDate getDayDateFromNumber(EventEntity event, Integer dayNumber) {
        if (event.getDays() != null && !event.getDays().isEmpty()) {
            return event.getDays().get(dayNumber - 1).getDate();
        }
        return event.getStartDateTime().toLocalDate();
    }

    private List<AttendanceStatsResponse.EventDaySchedule> buildEventSchedule(EventEntity event) {
        if (event.getDays() != null && !event.getDays().isEmpty()) {
            int dayNum = 1;
            List<AttendanceStatsResponse.EventDaySchedule> schedules = new ArrayList<>();
            for (EventDayEntity day : event.getDays()) {
                schedules.add(AttendanceStatsResponse.EventDaySchedule.builder()
                        .dayNumber(dayNum++).dayName(day.getDescription()).date(day.getDate()).build());
            }
            return schedules;
        }
        return List.of(AttendanceStatsResponse.EventDaySchedule.builder()
                .dayNumber(1).dayName("Day 1").date(event.getStartDateTime().toLocalDate()).build());
    }

    private List<AttendanceStatsResponse.DayStats> calculateDayStats(EventEntity event,
                                                                     List<EventBookingOrderEntity.BookedTicket> allTickets) {
        List<AttendanceStatsResponse.DayStats> stats = new ArrayList<>();
        int dayNum = 1;

        if (event.getDays() != null && !event.getDays().isEmpty()) {
            for (EventDayEntity day : event.getDays()) {
                stats.add(buildDayStats(day.getDescription(), day.getDate(), dayNum++, allTickets));
            }
        } else {
            stats.add(buildDayStats("Day 1", event.getStartDateTime().toLocalDate(), 1, allTickets));
        }
        return stats;
    }

    private AttendanceStatsResponse.DayStats buildDayStats(String dayName, LocalDate dayDate, int dayNumber,
                                                           List<EventBookingOrderEntity.BookedTicket> allTickets) {
        long checkedIn = allTickets.stream().filter(ticket -> ticket.isCheckedInForDay(dayName)).count();
        int total = allTickets.size();
        int absent = total - (int) checkedIn;
        double rate = total > 0 ? (checkedIn * 100.0) / total : 0.0;

        return AttendanceStatsResponse.DayStats.builder()
                .dayNumber(dayNumber).dayName(dayName).date(dayDate)
                .totalTickets(total).checkedIn((int) checkedIn).absent(absent)
                .attendanceRate(rate).status(determineDayStatus(dayDate)).build();
    }

    private List<AttendanceStatsResponse.TicketTypeStats> calculateTicketTypeStats(EventEntity event,
                                                                                   List<EventBookingOrderEntity.BookedTicket> allTickets) {
        Map<UUID, List<EventBookingOrderEntity.BookedTicket>> byType = allTickets.stream()
                .collect(Collectors.groupingBy(EventBookingOrderEntity.BookedTicket::getTicketTypeId));

        List<AttendanceStatsResponse.TicketTypeStats> stats = new ArrayList<>();

        for (Map.Entry<UUID, List<EventBookingOrderEntity.BookedTicket>> entry : byType.entrySet()) {
            TicketEntity ticketEntity = ticketRepo.findById(entry.getKey()).orElse(null);
            if (ticketEntity == null) continue;

            List<EventBookingOrderEntity.BookedTicket> tickets = entry.getValue();
            int totalSold = tickets.size();
            long checkedIn = tickets.stream().filter(EventBookingOrderEntity.BookedTicket::hasAnyCheckIn).count();
            int absent = totalSold - (int) checkedIn;
            double rate = totalSold > 0 ? (checkedIn * 100.0) / totalSold : 0.0;

            stats.add(AttendanceStatsResponse.TicketTypeStats.builder()
                    .ticketTypeId(entry.getKey()).ticketTypeName(ticketEntity.getName())
                    .totalSold(totalSold).totalCheckedIn((int) checkedIn).totalAbsent(absent)
                    .attendanceRate(rate).byDay(calculateTicketTypeDayStats(event, tickets)).build());
        }
        return stats;
    }

    private List<AttendanceStatsResponse.TicketTypeDayStats> calculateTicketTypeDayStats(EventEntity event,
                                                                                         List<EventBookingOrderEntity.BookedTicket> tickets) {
        List<AttendanceStatsResponse.TicketTypeDayStats> dayStats = new ArrayList<>();
        int dayNum = 1;

        if (event.getDays() != null && !event.getDays().isEmpty()) {
            for (EventDayEntity day : event.getDays()) {
                dayStats.add(buildTicketTypeDayStats(day.getDescription(), dayNum++, tickets));
            }
        } else {
            dayStats.add(buildTicketTypeDayStats("Day 1", 1, tickets));
        }
        return dayStats;
    }

    private AttendanceStatsResponse.TicketTypeDayStats buildTicketTypeDayStats(String dayName, int dayNumber,
                                                                               List<EventBookingOrderEntity.BookedTicket> tickets) {
        long checkedIn = tickets.stream().filter(ticket -> ticket.isCheckedInForDay(dayName)).count();
        int total = tickets.size();
        int absent = total - (int) checkedIn;
        double rate = total > 0 ? (checkedIn * 100.0) / total : 0.0;

        return AttendanceStatsResponse.TicketTypeDayStats.builder()
                .dayNumber(dayNumber).dayName(dayName)
                .checkedIn((int) checkedIn).absent(absent).attendanceRate(rate).build();
    }

    private String determineDayStatus(LocalDate dayDate) {
        LocalDate today = LocalDate.now();
        if (dayDate.isBefore(today)) return DayStatus.COMPLETED.name();
        if (dayDate.isEqual(today)) return DayStatus.ONGOING.name();
        return DayStatus.UPCOMING.name();
    }

    private AttendeeListResponse.AttendeeInfo mapToAttendeeInfo(EventBookingOrderEntity.BookedTicket ticket, String dayName) {
        EventBookingOrderEntity.BookedTicket.CheckInRecord checkIn = ticket.getCheckInsForDay(dayName).stream().findFirst().orElse(null);

        return AttendeeListResponse.AttendeeInfo.builder()
                .ticketInstanceId(ticket.getTicketInstanceId())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .attendeePhone(ticket.getAttendeePhone())
                .ticketType(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .pricePaid(ticket.getPrice())
                .checkInTime(checkIn != null ? checkIn.getCheckInTime() : null)
                .checkInLocation(checkIn != null ? checkIn.getCheckInLocation() : null)
                .checkedInBy(checkIn != null ? checkIn.getCheckedInBy() : null)
                .scannerId(checkIn != null ? checkIn.getScannerId() : null)
                .build();
    }

    private List<EventBookingOrderEntity.BookedTicket> filterByCategory(List<EventBookingOrderEntity.BookedTicket> absentees,
                                                                        String dayName, AbsenteeCategory category) {
        return absentees.stream()
                .filter(ticket -> {
                    boolean hasAnyCheckIn = ticket.hasAnyCheckIn();
                    return switch (category) {
                        case FULL_NO_SHOW -> !hasAnyCheckIn;
                        case SPECIFIC_DAY_ONLY -> hasAnyCheckIn && !ticket.isCheckedInForDay(dayName);
                        default -> true;
                    };
                })
                .toList();
    }

    private AbsenteeListResponse.AbsenteeInfo mapToAbsenteeInfo(EventBookingOrderEntity.BookedTicket ticket,
                                                                EventEntity event, int currentDayNumber) {
        int totalDays = event.getDays() != null && !event.getDays().isEmpty() ? event.getDays().size() : 1;

        List<Integer> attendedDayNumbers = new ArrayList<>();
        List<Integer> absentDayNumbers = new ArrayList<>();

        for (int i = 1; i <= totalDays; i++) {
            String dayName = getDayNameFromNumber(event, i);
            if (ticket.isCheckedInForDay(dayName)) {
                attendedDayNumbers.add(i);
            } else {
                absentDayNumbers.add(i);
            }
        }

        String category = ticket.hasAnyCheckIn() ? AbsenteeCategory.SPECIFIC_DAY_ONLY.name() : AbsenteeCategory.FULL_NO_SHOW.name();

        AbsenteeListResponse.AttendancePattern pattern = AbsenteeListResponse.AttendancePattern.builder()
                .totalEventDays(totalDays)
                .daysAttended(attendedDayNumbers.size())
                .daysAbsent(absentDayNumbers.size())
                .attendedDayNumbers(attendedDayNumbers)
                .absentDayNumbers(absentDayNumbers)
                .category(category)
                .build();

        return AbsenteeListResponse.AbsenteeInfo.builder()
                .ticketInstanceId(ticket.getTicketInstanceId())
                .attendeeName(ticket.getAttendeeName())
                .attendeeEmail(ticket.getAttendeeEmail())
                .attendeePhone(ticket.getAttendeePhone())
                .ticketType(ticket.getTicketTypeName())
                .ticketSeries(ticket.getTicketSeries())
                .pricePaid(ticket.getPrice())
                .statusForThisDay("NOT_CHECKED_IN")
                .attendancePattern(pattern)
                .build();
    }

    private List<AttendeeDetailResponse.DayCheckInInfo> buildCheckInHistory(EventEntity event,
                                                                            EventBookingOrderEntity.BookedTicket ticket) {
        List<AttendeeDetailResponse.DayCheckInInfo> history = new ArrayList<>();
        int dayNum = 1;

        if (event.getDays() != null && !event.getDays().isEmpty()) {
            for (EventDayEntity day : event.getDays()) {
                history.add(buildDayCheckInInfo(day.getDescription(), day.getDate(), dayNum++, ticket));
            }
        } else {
            history.add(buildDayCheckInInfo("Day 1", event.getStartDateTime().toLocalDate(), 1, ticket));
        }
        return history;
    }

    private AttendeeDetailResponse.DayCheckInInfo buildDayCheckInInfo(String dayName, LocalDate dayDate, int dayNumber,
                                                                      EventBookingOrderEntity.BookedTicket ticket) {
        String dayStatus = determineDayStatus(dayDate);
        EventBookingOrderEntity.BookedTicket.CheckInRecord checkIn = ticket.getCheckInsForDay(dayName).stream().findFirst().orElse(null);

        String status = checkIn != null ? "CHECKED_IN" :
                (dayStatus.equals(DayStatus.UPCOMING.name()) ? "UPCOMING" : "NOT_CHECKED_IN");

        return AttendeeDetailResponse.DayCheckInInfo.builder()
                .dayNumber(dayNumber).dayName(dayName).dayDate(dayDate).status(status)
                .checkInTime(checkIn != null ? checkIn.getCheckInTime() : null)
                .checkInLocation(checkIn != null ? checkIn.getCheckInLocation() : null)
                .checkedInBy(checkIn != null ? checkIn.getCheckedInBy() : null)
                .scannerId(checkIn != null ? checkIn.getScannerId() : null)
                .build();
    }

    private String determineOverallStatus(int daysAttended, int totalDays) {
        if (daysAttended == 0) return AttendanceStatus.NOT_ATTENDED.name();
        if (daysAttended == totalDays) return AttendanceStatus.FULLY_ATTENDED.name();
        return AttendanceStatus.PARTIALLY_ATTENDED.name();
    }

    private <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        if (start > list.size()) return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }

    private AttendeeListResponse.PaginationInfo buildPaginationInfo(Page<?> page) {
        return AttendeeListResponse.PaginationInfo.builder()
                .currentPage(page.getNumber()).pageSize(page.getSize())
                .totalPages(page.getTotalPages()).totalElements(page.getTotalElements())
                .hasNext(page.hasNext()).hasPrevious(page.hasPrevious()).build();
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

    private void validateEventOwnership(EventEntity event, AccountEntity user) throws AccessDeniedException {
        if (!event.getOrganizer().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only event organizer can view attendance");
        }
    }
}