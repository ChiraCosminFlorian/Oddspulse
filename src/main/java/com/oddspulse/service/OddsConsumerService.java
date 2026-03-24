package com.oddspulse.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oddspulse.dto.OddsFeedMessage;
import com.oddspulse.model.OddsEvent;
import com.oddspulse.model.OddsSnapshot;
import com.oddspulse.repository.OddsEventRepository;
import com.oddspulse.repository.OddsSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OddsConsumerService {

    private final AmazonSQS sqsClient;
    private final AmazonS3 s3Client;
    private final OddsEventRepository eventRepository;
    private final OddsSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.aws.sqs-queue-url}")
    private String sqsQueueUrl;

    @Value("${app.aws.s3-bucket}")
    private String s3Bucket;

    @Scheduled(fixedRateString = "${app.odds-feed.poll-interval-ms}")
    public void pollSqsQueue() {
        ReceiveMessageRequest receiveRequest = new ReceiveMessageRequest()
                .withQueueUrl(sqsQueueUrl)
                .withMaxNumberOfMessages(10)
                .withWaitTimeSeconds(2);

        List<Message> messages = sqsClient.receiveMessage(receiveRequest).getMessages();

        for (Message message : messages) {
            try {
                OddsFeedMessage feedMessage = objectMapper.readValue(message.getBody(), OddsFeedMessage.class);
                processOddsMessage(feedMessage);
                sqsClient.deleteMessage(sqsQueueUrl, message.getReceiptHandle());
                log.info("Processed odds update for event: {}", feedMessage.getEventId());
            } catch (Exception e) {
                log.error("Failed to process SQS message: {}", message.getMessageId(), e);
            }
        }
    }

    private void processOddsMessage(OddsFeedMessage message) {
        OddsEvent event = eventRepository.findByEventId(message.getEventId())
                .map(existing -> {
                    existing.setStatus("LIVE");
                    existing.setUpdatedAt(LocalDateTime.now());
                    return eventRepository.save(existing);
                })
                .orElseGet(() -> {
                    OddsEvent newEvent = OddsEvent.builder()
                            .eventId(message.getEventId())
                            .sportType(message.getSportType())
                            .homeTeam(message.getHomeTeam())
                            .awayTeam(message.getAwayTeam())
                            .eventTime(message.getEventTime())
                            .status("UPCOMING")
                            .build();
                    return eventRepository.save(newEvent);
                });

        OddsSnapshot snapshot = OddsSnapshot.builder()
                .event(event)
                .homeOdds(message.getHomeOdds())
                .drawOdds(message.getDrawOdds())
                .awayOdds(message.getAwayOdds())
                .totalVolume(message.getTotalVolume())
                .snapshotTime(LocalDateTime.now())
                .source("FEED_SIMULATOR")
                .build();

        snapshotRepository.save(snapshot);

        archiveSnapshotToS3(event, snapshot);
    }

    @Async
    protected void archiveSnapshotToS3(OddsEvent event, OddsSnapshot snapshot) {
        try {
            String jsonContent = objectMapper.writeValueAsString(snapshot);
            String snapshotTimeFormatted = snapshot.getSnapshotTime().toString()
                    .replace(":", "-")
                    .replace(" ", "-");
            String key = String.format("snapshots/%s/%s/%s.json",
                    event.getSportType(), event.getEventId(), snapshotTimeFormatted);

            s3Client.putObject(s3Bucket, key, jsonContent);
            log.info("Snapshot archived to S3: {}", key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize snapshot for S3 archival: {}", event.getEventId(), e);
        } catch (Exception e) {
            log.error("Failed to archive snapshot to S3 for event: {}", event.getEventId(), e);
        }
    }
}
