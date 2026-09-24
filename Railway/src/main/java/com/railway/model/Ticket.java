package com.railway.model;

import java.math.BigDecimal;

public record Ticket(long id, long bookingId, int seatNumber, BigDecimal price, String reference) {}
