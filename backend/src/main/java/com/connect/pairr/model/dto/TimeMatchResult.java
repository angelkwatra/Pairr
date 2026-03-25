package com.connect.pairr.model.dto;

import lombok.Builder;

@Builder
public record TimeMatchResult(
        long bestOverlap,
        long bestDistance
) {}