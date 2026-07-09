package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 익명화 요청을 위한 DTO입니다.
 */
public record AnonymizationRequestDto (@JsonProperty("study-uid") String studyUid) {}