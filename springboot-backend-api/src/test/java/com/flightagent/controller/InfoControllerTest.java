package com.flightagent.controller;

import com.flightagent.tools.WeatherToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class InfoControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean WeatherToolService weatherToolService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void getWeatherReturnsTemp() throws Exception {
        when(weatherToolService.getDestinationWeather("Tokyo"))
                .thenReturn(Map.of(
                        "source", "sample_data",
                        "city", "Tokyo",
                        "temp_c", 21.0,
                        "conditions", "Partly cloudy",
                        "humidity", 60));

        mockMvc.perform(get("/api/weather/Tokyo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Tokyo"))
                .andExpect(jsonPath("$.temp_c").value(21.0));
    }

    @Test
    void getDestinationReturnsCuratedInfo() throws Exception {
        when(weatherToolService.getDestinationInfo("London"))
                .thenReturn(Map.of(
                        "city", "London",
                        "info", Map.of("country", "UK", "currency", "GBP")));

        mockMvc.perform(get("/api/destination/London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.currency").value("GBP"));
    }
}
