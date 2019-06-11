package org.springframework.cloud.stream.app.demo.webflux.processor;

import java.net.URI;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("demo")
@Validated
@Getter
@Setter
public class DemoWebfluxProcessorProperties {
    /**
     * URI to send demo data
     */
    @NotNull
    private URI targetUri;
}

