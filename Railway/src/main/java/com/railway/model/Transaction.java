package com.railway.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
    long id, long paymentId, String type, BigDecimal amount, LocalDateTime createdAt) {}
