package com.oddspulse.service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oddspulse.dto.OddsFeedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class OddsFeedSimulatorService {

    private final AmazonSQS sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${app.aws.sqs-queue-url}")
    private String sqsQueueUrl;

    private static final List<String[]> SAMPLE_EVENTS = List.of(
            new String[]{"EVT001", "FOOTBALL", "Manchester City", "Arsenal"},
            new String[]{"EVT002", "TENNIS", "Djokovic", "Alcaraz"},
            new String[]{"EVT003", "BASKETBALL", "Lakers", "Celtics"}
    );

    @Scheduled(fixedRateString = "${app.odds-feed.poll-interval-ms}")
    public void simulateFeedUpdate() {
        for (String[] event : SAMPLE_EVENTS) {
            String eventId = event[0];
            String sportType = event[1];
            String homeTeam = event[2];
            String awayTeam = event[3];

            OddsFeedMessage message = OddsFeedMessage.builder()
                    .eventId(eventId)
                    .sportType(sportType)
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .homeOdds(generateRandomOdds(1.10, 5.00))
                    .awayOdds(generateRandomOdds(1.10, 5.00))
                    .drawOdds("TENNIS".equals(sportType) ? null : generateRandomOdds(2.50, 4.00))
                    .totalVolume(generateRandomOdds(10000, 500000))
                    .eventTime(LocalDateTime.now().plusHours(ThreadLocalRandom.current().nextInt(1, 49)))
                    .build();

            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                sqsClient.sendMessage(sqsQueueUrl, jsonMessage);
                log.info("Feed update sent for event: {}", eventId);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize feed message for event: {}", eventId, e);
            }
        }
    }

    private BigDecimal generateRandomOdds(double min, double max) {
        double value = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
