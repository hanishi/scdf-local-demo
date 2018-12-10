package org.springframework.cloud.stream.app.demo.load.generator.source;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

public class MessageEventProducerWithTranslator<T> {
    private final RingBuffer<MessageEvent<T>> ringBuffer;
    private final EventTranslatorOneArg<MessageEvent<T>, T> TRANSLATOR =
            (messageEvent, sequence, tMessage) -> messageEvent.setMessage(tMessage);

    public MessageEventProducerWithTranslator(RingBuffer<MessageEvent<T>> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    public void onData(T message) {
        ringBuffer.publishEvent(TRANSLATOR, message);
    }
}
