package org.springframework.cloud.stream.app.demo.webflux.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.webflux.dsl.WebFlux;
import org.springframework.messaging.MessageHeaders;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

@Configuration
@EnableBinding(Processor.class)
@ComponentScan
@Slf4j
public class DemoWebfluxProcessorConfiguration {

    private static final String HEADER_USER_ID = "user_id";

    private static final String HEADER_ITEMS = "items";

    @Bean
    public IntegrationFlow enrichContent(ObjectMapper objectMapper, DemoWebfluxProcessorProperties properties) {

        return IntegrationFlows.from(Processor.INPUT)
                .<byte[], JsonNode>transform(p -> {
                    //　この部分は、Spring IntegrationのJava DSLを使った場合に必要、Spring Integration 5.1だと必要ない。
                    try {
                        return objectMapper.readTree(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    throw new RuntimeException();
                })
                .enrich(enricherSpec -> enricherSpec
                        // HEADER_USER_IDにJSONの id＝123456789を記録します。
                        .<JsonNode>headerFunction(HEADER_USER_ID, m -> m.getPayload().path("id").asInt())
                        // HEADER_ITEMSに"items"の個数を記録します。
                        // ここで付与したヘッダーは、後続の処理でメッセージが新たに生成された場合、必ずコピーされます。
                        .headerExpression(HEADER_ITEMS, "payload.items.size()"))
                .split(JsonNode.class, jsonNode -> {
                    //
                    // {"id": 123456789,"items": [{"id": 1,"name": "jeans"},{"id": 2, "name": "t-shirt"},{"id": 3, "name": "shoes"}]}
                    // このJSONをここでsplit(Iteratorを返します。)します。
                    //
                    ArrayNode arrayNode = (ArrayNode) jsonNode.path("items");
                    return arrayNode.iterator();
                })
                .channel(MessageChannels.executor(Executors.newSingleThreadExecutor()))
                .<JsonNode>handle((p, m) -> {
                    ObjectNode objectNode = p.deepCopy();
                    //
                    // ここは、splitで返したIteratorが終わるまで、実行されます。この前にExecutors.newSingleThreadExecutor()
                    // を使っていますが、Unitテストを通すだけのためにしています。本来は、item在庫確認は並列処理で行います
                    return objectNode.put("available", true);
                })
                .aggregate(aggregatorSpec -> aggregatorSpec
                        // group.getOne()を使うとMessageGroupに入ってるメッセージの１つにアクセスできます。
                        .releaseStrategy(group -> group.size() ==
                                Objects.requireNonNull(group.getOne().getHeaders().get(HEADER_ITEMS, Integer.class)))
                        .outputProcessor(group -> {
                            ArrayNode items = group.getMessages().stream()
                                    .map(m -> (JsonNode) m.getPayload())
                                    .collect(new ArrayNodeCollector(objectMapper));
                            Integer id = group.getOne().getHeaders().get(HEADER_USER_ID, Integer.class);
                            ObjectNode objectNode = objectMapper.createObjectNode();
                            objectNode.put("id", id)
                                    .set("items", items);
                            return objectNode;
                        })).enrichHeaders(h -> h.header(MessageHeaders.CONTENT_TYPE, "application/json"))
                .handle(WebFlux.<String>outboundGateway(m -> properties.getTargetUri()).httpMethod(HttpMethod.POST)
                        .expectedResponseType(JsonNode.class))
                .channel(Processor.OUTPUT).get();
    }

    public static class ArrayNodeCollector implements Collector<JsonNode, ArrayNode, ArrayNode> {

        private final ObjectMapper objectMapper;

        public ArrayNodeCollector(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Supplier<ArrayNode> supplier() {
            return objectMapper::createArrayNode;
        }

        @Override
        public BiConsumer<ArrayNode, JsonNode> accumulator() {
            return ArrayNode::add;
        }

        @Override
        public BinaryOperator<ArrayNode> combiner() {
            return (x, y) -> {
                x.addAll(y);
                return x;
            };
        }

        @Override
        public Function<ArrayNode, ArrayNode> finisher() {
            return accumulator -> accumulator;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return EnumSet.of(Characteristics.UNORDERED);
        }

    }

}
