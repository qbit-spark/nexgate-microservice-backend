package org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.entity.EventBookingOrderEntity;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload.BookingOrderResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.payload.BookingOrderSummaryResponse;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.service.EventBookingOrderService;
import org.nextgate.nextgatebackend.e_events.events_mng.event_booking_order.utils.mapper.BookingOrderMapper;
import org.nextgate.nextgatebackend.globeadvice.exceptions.AccessDeniedException;
import org.nextgate.nextgatebackend.globeadvice.exceptions.ItemNotFoundException;
import org.nextgate.nextgatebackend.globeresponsebody.GlobeSuccessResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/e-events/booking-orders")
@RequiredArgsConstructor
@Slf4j
public class EventBookingOrderController {

    private final EventBookingOrderService bookingOrderService;

    @GetMapping("/{bookingId}")
    public ResponseEntity<GlobeSuccessResponseBuilder> getBookingById(@PathVariable UUID bookingId)
            throws ItemNotFoundException, AccessDeniedException {

        EventBookingOrderEntity booking = bookingOrderService.getBookingById(bookingId);
        BookingOrderResponse response = BookingOrderMapper.toResponse(booking);


        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Booking retrieved successfully")
                        .data(response)
                        .build());
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<GlobeSuccessResponseBuilder> getMyBookings()
            throws ItemNotFoundException {


        List<EventBookingOrderEntity> bookings = bookingOrderService.getMyBookings();

        List<BookingOrderSummaryResponse> response = bookings.stream()
                .map(BookingOrderMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(GlobeSuccessResponseBuilder.builder()
                        .success(true)
                        .httpStatus(HttpStatus.OK)
                        .message("Bookings retrieved successfully")
                        .data(response)
                        .build());
    }
}