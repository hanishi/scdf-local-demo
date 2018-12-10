package org.springframework.cloud.stream.app.demo.load.generator.source;

import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

enum Payload {

    JSON("data.json");

    @Getter
    private final Resource resource;

    Payload(String jsonFilePath) {
        this.resource = resource(jsonFilePath);
    }

    private Resource resource(String filename) {
        return new ClassPathResource("/payload/" + filename, getClass());
    }
}
