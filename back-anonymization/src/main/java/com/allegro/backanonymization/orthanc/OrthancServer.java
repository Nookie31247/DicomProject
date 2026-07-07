package com.allegro.backanonymization.orthanc;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class OrthancServer {
    private final WebClient orthancWebClient;

    public 
}
