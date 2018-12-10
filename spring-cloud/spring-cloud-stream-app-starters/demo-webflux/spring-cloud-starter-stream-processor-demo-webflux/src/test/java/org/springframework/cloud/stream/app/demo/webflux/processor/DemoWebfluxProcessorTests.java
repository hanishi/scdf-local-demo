package org.springframework.cloud.stream.app.demo.webflux.processor;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext
@Slf4j
public abstract class DemoWebfluxProcessorTests {

    @Autowired
    protected Processor channels;

    @Autowired
    protected MessageCollector messageCollector;

    @Autowired
    protected ObjectMapper objectMapper;

    @TestPropertySource(properties = {
            "server.port=1234",
            "demo.targetUri=http://localhost:1234/post"
    })
    public static class WebFluxOutboundGatewayTest extends DemoWebfluxProcessorTests {

        @Test
        public void webFluxOutboundGatewayTestSample() throws Exception {

            String order =
                    "{\"id\":123456789,"
                            + "\"items\":["
                            + "{\"id\":1,\"name\":\"jeans\"},"
                            + "{\"id\":2,\"name\":\"t-shirt\"},"
                            + "{\"id\":3,\"name\":\"shoes\"}"
                            + "]}";
            String orderChecked =
                    "{\"id\":123456789,"
                            + "\"items\":["
                            + "{\"id\":1,\"name\":\"jeans\",\"available\":true},"
                            + "{\"id\":2,\"name\":\"t-shirt\",\"available\":true},"
                            + "{\"id\":3,\"name\":\"shoes\",\"available\":true}"
                            + "]}";

            Message message = MessageBuilder.withPayload(order.getBytes()).build();

            channels.input().send(message);
            assertThat(messageCollector.forChannel(channels.output()),
                    receivesPayloadThat(equalTo(orderChecked)));
        }

    }

    @RestController
    public static class TestController {

        @RequestMapping(value = "/post", method = RequestMethod.POST)
        public JsonNode post(@RequestBody JsonNode payload) {
            return payload;
        }
    }


    @SpringBootApplication
    @Import(TestController.class)
    public static class HttpClientProcessorApplication {

    }
}
