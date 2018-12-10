package org.springframework.cloud.stream.app.demo.load.generator.source;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("demo-load-generator")
public class DemoLoadGeneratorSourceProperties {

    @NotNull
    @Getter
    @Setter
    private Payload payload;

    @Getter
    @Setter
    private Integer interval = 0;
}
