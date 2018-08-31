package no.nav.paop

import org.apache.kafka.common.serialization.Serializer

import java.util.Properties
import kotlin.reflect.KClass

fun readConsumerConfig(
    env: Environment
) = Properties().apply {
    load(Environment::class.java.getResourceAsStream("/kafka_consumer.properties"))
    this["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${env.srvPaopUsername}\" password=\"${env.srvPaopPassword}\";"
    this["bootstrap.servers"] = env.kafkaBootstrapServersURL
}

fun readProducerConfig(
    env: Environment,
    valueSerializer: KClass<out Serializer<out Any>>,
    keySerializer: KClass<out Serializer<out Any>> = valueSerializer
) = Properties().apply {
    load(Environment::class.java.getResourceAsStream("/kafka_producer.properties"))
    this["sasl.jaas.config"] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"${env.srvPaopUsername}\" password=\"${env.srvPaopPassword}\";"
    this["key.serializer"] = keySerializer.qualifiedName
    this["value.serializer"] = valueSerializer.qualifiedName
    this["bootstrap.servers"] = env.kafkaBootstrapServersURL
}
