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
public class PostPaymentHandlerRegistry {

    private final Map<String, PostPaymentHandler> handlers;

    public PostPaymentHandlerRegistry(List<PostPaymentHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        PostPaymentHandler::getSupportedDomain,
                        Function.identity()
                ));

        log.info("Registered {} post-payment handlers: {}",
                handlers.size(), handlers.keySet());
    }

    public PostPaymentHandler getHandler(String domain) throws RandomExceptions {
        PostPaymentHandler handler = handlers.get(domain);

        if (handler == null) {
            throw new RandomExceptions("No post-payment handler found for domain: " + domain);
        }

        return handler;
    }

    public boolean hasHandler(String domain) {
        return handlers.containsKey(domain);
    }
}