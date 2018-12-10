package org.springframework.cloud.stream.app.demo.load.generator.source;

import com.lmax.disruptor.EventFactory;

public class MessageEventFactory<T> implements EventFactory<MessageEvent<T>> {
    @Override
    public MessageEvent<T> newInstance() {
        return new MessageEvent<>();
    }
}
