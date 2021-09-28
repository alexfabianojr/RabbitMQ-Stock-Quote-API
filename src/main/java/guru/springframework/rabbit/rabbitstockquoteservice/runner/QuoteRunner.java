package guru.springframework.rabbit.rabbitstockquoteservice.runner;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import guru.springframework.rabbit.rabbitstockquoteservice.config.RabbitConfig;
import guru.springframework.rabbit.rabbitstockquoteservice.sender.QuoteMessageSender;
import guru.springframework.rabbit.rabbitstockquoteservice.service.QuoteGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.Receiver;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteRunner implements CommandLineRunner {

    private final QuoteGeneratorService quoteGeneratorService;
    private final QuoteMessageSender quoteMessageSender;
    //private final Flux<Delivery>  deliveryFlux;
    private final Receiver receiver;

    @Override
    public void run(String... args) throws Exception {

        CountDownLatch latch = new CountDownLatch(25);

        quoteGeneratorService
                .fetchQuoteStream(Duration.ofMillis(100L))
                .take(25)
                .flatMap(quote -> {
                    final Mono<Void> voidMono = quoteMessageSender.sendQuoteMessage(quote);
                    log.debug("Sent: " + quote);
                    return voidMono;
                })
                .subscribe(result -> {
                            log.debug("result: " + result);
                            latch.countDown();
                        },
                        throwable -> log.error("Error > ", throwable),
                        () -> log.debug("All done"));

        latch.await(5, TimeUnit.SECONDS);

        AtomicInteger receivedCount = new AtomicInteger();

        receiver.consumeAutoAck(RabbitConfig.QUEUE)
                .log("Msg Receiver")
                .subscribe(msg -> {
                    log.debug("Received Message # {} - {}", receivedCount.incrementAndGet(), new String(msg.getBody()));
                }, throwable -> {
                    log.debug("Error Receiving", throwable);
                }, () -> {
                    log.debug("Complete");
                });
    }
}
