package org.springframework.cloud.stream.app.demo.load.generator.source;

public class MessageEvent<T> {
    private T message;

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
