package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AnonymizationRequestDto (@JsonProperty("study-uid") String studyUid) {}