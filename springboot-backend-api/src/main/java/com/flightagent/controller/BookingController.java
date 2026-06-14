package com.flightagent.controller;

import com.flightagent.dto.request.BookRequest;
import com.flightagent.dto.request.CancelRequest;
import com.flightagent.dto.request.ModifyRequest;
import com.flightagent.exception.NotFoundException;
import com.flightagent.tools.BookingToolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingToolService bookingToolService;

    public BookingController(BookingToolService bookingToolService) {
        this.bookingToolService = bookingToolService;
    }

    @PostMapping("/book")
    public Map<String, Object> book(@Valid @RequestBody BookRequest req) {
        return bookingToolService.bookFlight(
                req.offerId(),
                req.passengerName(),
                req.passengerEmail(),
                req.origin(),
                req.destination(),
                req.departureDate(),
                req.priceTotal(),
                req.currencyOrDefault());
    }

    @GetMapping("/booking/{bookingId}")
    public Map<String, Object> bookingDetail(@PathVariable String bookingId) {
        Map<String, Object> result = bookingToolService.getBooking(bookingId);
        if (result.containsKey("error")) {
            throw new NotFoundException(String.valueOf(result.get("error")));
        }
        return result;
    }

    @PostMapping("/booking/{bookingId}/cancel")
    public Map<String, Object> cancel(
            @PathVariable String bookingId,
            @RequestBody(required = false) CancelRequest req) {
        String reason = req == null ? null : req.reason();
        return bookingToolService.cancelBooking(bookingId, reason);
    }

    @PostMapping("/booking/{bookingId}/modify")
    public Map<String, Object> modify(
            @PathVariable String bookingId,
            @RequestBody ModifyRequest req) {
        return bookingToolService.modifyBooking(
                bookingId, req.newDepartureDate(), req.newDestination());
    }
}
