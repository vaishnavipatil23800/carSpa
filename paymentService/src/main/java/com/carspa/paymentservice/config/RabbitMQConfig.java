package com.carspa.paymentservice.config;

/**
 * RabbitMQConfig — disabled until RabbitMQ is installed.
 */
public class RabbitMQConfig {
    public static final String PAYMENT_EXCHANGE    = "carspa.payment.exchange";
    public static final String PAYMENT_QUEUE       = "carspa.payment.queue";
    public static final String PAYMENT_ROUTING_KEY = "payment.#";
}