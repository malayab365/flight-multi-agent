package com.flightagent.tools;

import com.flightagent.entity.PriceWatchEntity;
import com.flightagent.entity.WatchStatus;
import com.flightagent.repository.PriceWatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PriceToolService {

    private final PriceWatchRepository priceWatchRepository;
    private final IdGenerator idGenerator;

    public PriceToolService(PriceWatchRepository priceWatchRepository, IdGenerator idGenerator) {
        this.priceWatchRepository = priceWatchRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Map<String, Object> trackPrice(
            String origin, String destination, String departureDate,
            double targetPrice, String email) {

        String watchId = idGenerator.newId("PW");
        PriceWatchEntity watch = new PriceWatchEntity();
        watch.setWatchId(watchId);
        watch.setOrigin(origin);
        watch.setDestination(destination);
        watch.setDepartureDate(departureDate);
        watch.setTargetPrice(BigDecimal.valueOf(targetPrice).setScale(2, RoundingMode.HALF_UP));
        watch.setEmail(email);
        watch.setStatus(WatchStatus.ACTIVE);
        watch.setCreatedAt(Instant.now());
        priceWatchRepository.save(watch);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("watch_id", watchId);
        out.put("status", "ACTIVE");
        out.put("message", "Tracking " + origin + "->" + destination
                + " on " + departureDate
                + "; will alert " + email + " at <= " + targetPrice + ".");
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> checkPriceWatch(String watchId) {
        Optional<PriceWatchEntity> opt = priceWatchRepository.findById(watchId);
        if (opt.isEmpty()) {
            return Map.of("error", "No price watch with id " + watchId);
        }
        PriceWatchEntity watch = opt.get();
        double target = watch.getTargetPrice().doubleValue();
        double current = round2(target * ThreadLocalRandom.current().nextDouble(0.85, 1.25));
        boolean triggered = current <= target;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("watch_id", watchId);
        out.put("route", watch.getOrigin() + "-" + watch.getDestination());
        out.put("current_price", current);
        out.put("target_price", target);
        out.put("alert_triggered", triggered);
        out.put("action", triggered ? "Notify user" : "Keep watching");
        return out;
    }

    public Map<String, Object> getPriceHistory(String origin, String destination, String departureDate) {
        long base = 200L + Math.floorMod((long) (origin + destination).hashCode(), 300L);

        Map<String, Object> quartiles = new LinkedHashMap<>();
        quartiles.put("minimum", round2(base * 0.7));
        quartiles.put("first", round2(base * 0.85));
        quartiles.put("median", (double) base);
        quartiles.put("third", round2(base * 1.2));
        quartiles.put("maximum", round2(base * 1.6));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("route", origin + "-" + destination);
        out.put("departure_date", departureDate);
        out.put("currency", "USD");
        out.put("quartiles", quartiles);
        out.put("note", "Sample distribution; wire to Amadeus Flight Price Analysis.");
        return out;
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
