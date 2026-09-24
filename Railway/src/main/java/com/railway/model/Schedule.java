package com.railway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Schedule(
    long id,
    Train train,
    LocalDateTime departure,
    LocalDateTime arrival,
    BigDecimal fare,
    int capacity,
    int availableSeats) {}
