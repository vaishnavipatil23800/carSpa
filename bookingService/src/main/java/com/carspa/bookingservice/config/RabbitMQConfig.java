package com.carspa.bookingservice.config;

/**
 * RabbitMQConfig — disabled until RabbitMQ is installed.
 * To enable: install RabbitMQ, set rabbitmq.enabled=true in application.properties
 */
public class RabbitMQConfig {
    public static final String BOOKING_EXCHANGE    = "carspa.booking.exchange";
    public static final String BOOKING_QUEUE       = "carspa.booking.queue";
    public static final String BOOKING_ROUTING_KEY = "booking.#";
}