package org.qubership.integration.platform.variables.management.rest.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class StaticFileController {

    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Resource> getStaticApiDocs() {
        return Mono.just(new ClassPathResource("static/api-docs.json"));
    }
}
