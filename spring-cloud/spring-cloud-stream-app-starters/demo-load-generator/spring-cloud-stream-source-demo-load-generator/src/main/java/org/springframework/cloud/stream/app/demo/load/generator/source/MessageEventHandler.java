package org.springframework.cloud.stream.app.demo.load.generator.source;

import com.lmax.disruptor.EventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
public class MessageEventHandler<T> implements EventHandler<MessageEvent<T>> {
    private Source channel;

    MessageEventHandler(Source channel) {
        this.channel = channel;
    }

    @Override
    public void onEvent(MessageEvent<T> messageEvent, long sequence, boolean endOfBatch) throws Exception {
        Message<T> message = MessageBuilder.withPayload(messageEvent.getMessage()).build();
        channel.output().send(message);
    }
}
