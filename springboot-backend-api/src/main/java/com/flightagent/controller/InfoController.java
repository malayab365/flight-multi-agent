package com.flightagent.controller;

import com.flightagent.tools.WeatherToolService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InfoController {

    private final WeatherToolService weatherToolService;

    public InfoController(WeatherToolService weatherToolService) {
        this.weatherToolService = weatherToolService;
    }

    @GetMapping("/weather/{city}")
    public Map<String, Object> weather(@PathVariable String city) {
        return weatherToolService.getDestinationWeather(city);
    }

    @GetMapping("/destination/{city}")
    public Map<String, Object> destinationInfo(@PathVariable String city) {
        return weatherToolService.getDestinationInfo(city);
    }
}
