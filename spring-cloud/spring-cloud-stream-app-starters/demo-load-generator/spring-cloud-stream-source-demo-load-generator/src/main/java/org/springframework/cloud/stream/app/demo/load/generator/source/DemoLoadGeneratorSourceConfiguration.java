package org.springframework.cloud.stream.app.demo.load.generator.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.AbstractEndpoint;

@EnableBinding(Source.class)
@EnableConfigurationProperties({DemoLoadGeneratorSourceProperties.class})
@Configuration
public class DemoLoadGeneratorSourceConfiguration extends AbstractEndpoint {

    static final Log logger = LogFactory.getLog(DemoLoadGeneratorSourceConfiguration.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    DemoLoadGeneratorSourceProperties properties;

    @Autowired
    Source channel;

    ObjectMapper objectMapper = new ObjectMapper();

    private ExecutorService executorService;

    @Override
    protected void doStart() {
        if (running.compareAndSet(false, true)) {
            executorService = Executors.newSingleThreadExecutor();
            MessageEventFactory<String> factory = new MessageEventFactory<>();
            int bufferSize = 1024;
            Disruptor<MessageEvent<String>> disruptor = new Disruptor<>(factory, bufferSize,
                    Executors.defaultThreadFactory());
            Integer interval = properties.getInterval();
            disruptor.handleEventsWith(new MessageEventHandler<>(channel));
            disruptor.start();

            RingBuffer<MessageEvent<String>> ringBuffer = disruptor.getRingBuffer();
            MessageEventProducerWithTranslator<String> producer = new MessageEventProducerWithTranslator<>(
                    ringBuffer);
            Payload payload = properties.getPayload();
            try {
                String json = objectMapper.readTree(payload.getResource().getInputStream()).toString();
                executorService.submit(() -> {
                    while (running.get()) {
                        producer.onData(json);
                        try {
                            Thread.sleep(interval);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void doStop() {
        if (running.compareAndSet(true, false)) {
            logger.info(String.format("Terminating load generator"));
            executorService.shutdown();
        }
    }
}