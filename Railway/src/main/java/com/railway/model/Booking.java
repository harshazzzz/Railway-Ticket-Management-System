package com.railway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Booking(
    long id,
    long userId,
    long scheduleId,
    String passenger,
    String train,
    String origin,
    String destination,
    LocalDateTime departure,
    String status,
    int seatNumber,
    BigDecimal price,
    String reference,
    String paymentStatus) {}
