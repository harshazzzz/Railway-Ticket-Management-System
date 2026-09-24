package com.railway.model;

public record Train(
    long id, String name, String origin, String destination, int capacity, boolean active) {}
