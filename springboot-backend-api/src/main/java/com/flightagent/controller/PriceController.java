package com.flightagent.controller;

import com.flightagent.dto.request.PriceTrackRequest;
import com.flightagent.tools.PriceToolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/price")
public class PriceController {

    private final PriceToolService priceToolService;

    public PriceController(PriceToolService priceToolService) {
        this.priceToolService = priceToolService;
    }

    @PostMapping("/track")
    public Map<String, Object> track(@Valid @RequestBody PriceTrackRequest req) {
        return priceToolService.trackPrice(
                req.origin(),
                req.destination(),
                req.departureDate(),
                req.targetPrice(),
                req.email());
    }

    @GetMapping("/watch/{watchId}")
    public Map<String, Object> watch(@PathVariable String watchId) {
        return priceToolService.checkPriceWatch(watchId);
    }

    @GetMapping("/history")
    public Map<String, Object> history(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("departure_date") String departureDate) {
        return priceToolService.getPriceHistory(origin, destination, departureDate);
    }
}
