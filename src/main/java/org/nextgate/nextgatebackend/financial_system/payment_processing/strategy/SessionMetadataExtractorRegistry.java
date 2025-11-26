package org.nextgate.nextgatebackend.financial_system.payment_processing.strategy;

import lombok.extern.slf4j.Slf4j;
import org.nextgate.nextgatebackend.globeadvice.exceptions.RandomExceptions;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SessionMetadataExtractorRegistry {

    private final Map<String, SessionMetadataExtractor> extractors;

    public SessionMetadataExtractorRegistry(List<SessionMetadataExtractor> extractorList) {
        this.extractors = extractorList.stream()
                .collect(Collectors.toMap(
                        SessionMetadataExtractor::getSupportedDomain,
                        Function.identity()
                ));

        log.info("Registered {} session metadata extractors: {}",
                extractors.size(), extractors.keySet());
    }

    public SessionMetadataExtractor getExtractor(String domain) throws RandomExceptions {
        SessionMetadataExtractor extractor = extractors.get(domain);

        if (extractor == null) {
            throw new RandomExceptions("No metadata extractor found for domain: " + domain);
        }

        return extractor;
    }

    public boolean hasExtractor(String domain) {
        return extractors.containsKey(domain);
    }
}