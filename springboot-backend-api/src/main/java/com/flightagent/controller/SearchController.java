package com.flightagent.controller;

import com.flightagent.dto.request.SearchRequest;
import com.flightagent.tools.SearchToolService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchToolService searchToolService;

    public SearchController(SearchToolService searchToolService) {
        this.searchToolService = searchToolService;
    }

    @PostMapping("/search")
    public Map<String, Object> search(@Valid @RequestBody SearchRequest req) {
        return searchToolService.searchFlights(
                req.origin(),
                req.destination(),
                req.departureDate(),
                req.returnDate(),
                req.adultsOrDefault(),
                req.travelClassOrDefault(),
                req.maxResultsOrDefault());
    }

    @GetMapping("/airport-code")
    public Map<String, Object> airportCode(@RequestParam("city") String city) {
        return searchToolService.getAirportCode(city);
    }
}
