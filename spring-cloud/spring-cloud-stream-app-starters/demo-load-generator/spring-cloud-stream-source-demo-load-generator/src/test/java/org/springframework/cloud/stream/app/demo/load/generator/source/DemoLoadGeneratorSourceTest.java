package org.springframework.cloud.stream.app.demo.load.generator.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext
@Slf4j
public abstract class DemoLoadGeneratorSourceTest {

    @Autowired
    protected Source channels;

    @Autowired
    protected MessageCollector messageCollector;

    @TestPropertySource(properties = {
            "demo-load-generator.payload=JSON",
            "demo-load-generator.interval=100"
    })
    public static class AppTests extends DemoLoadGeneratorSourceTest {

        @Test
        public void testMessageLoading() throws Exception {
            Thread.sleep(5000);
            List<Message<?>> messages = new ArrayList<>();
            messageCollector.forChannel(channels.output()).drainTo(messages, 5);
            assertEquals(5, messages.size());
            messages.forEach(message -> {
                assertNotNull(message.getPayload());
            });
        }

    }

    @SpringBootApplication
    public static class LoadGeneratorSourceApplication {

    }

}