package com.flightagent.controller;

import com.flightagent.dto.request.BaggageRequest;
import com.flightagent.dto.request.SeatRequest;
import com.flightagent.tools.AncillaryToolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AncillaryController {

    private final AncillaryToolService ancillaryToolService;

    public AncillaryController(AncillaryToolService ancillaryToolService) {
        this.ancillaryToolService = ancillaryToolService;
    }

    @GetMapping("/seat-map/{offerId}")
    public Map<String, Object> seatMap(@PathVariable String offerId) {
        return ancillaryToolService.getSeatMap(offerId);
    }

    @PostMapping("/booking/{bookingId}/seat")
    public Map<String, Object> assignSeat(
            @PathVariable String bookingId,
            @Valid @RequestBody SeatRequest req) {
        return ancillaryToolService.selectSeat(bookingId, req.seatType(), req.seatNumber());
    }

    @PostMapping("/booking/{bookingId}/baggage")
    public Map<String, Object> addBaggage(
            @PathVariable String bookingId,
            @Valid @RequestBody BaggageRequest req) {
        return ancillaryToolService.addBaggage(bookingId, req.baggageType(), req.quantityOrDefault());
    }
}
