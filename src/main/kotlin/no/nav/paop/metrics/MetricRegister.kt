package no.nav.paop.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "paop"

val NETWORK_CALL_TIME: Summary = Summary.Builder()
        .namespace(METRICS_NS)
        .name("network_call_time")
        .labelNames("service")
        .help("Time it takes to execute a call over network").register()

val REQUEST_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("request_time_ms")
        .help("Request time in milliseconds.").register()

val INCOMING_MESSAGE_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("incoming_message_count")
        .labelNames("message_type")
        .help("Counts the number of incoming messages")
        .register()

val RETRY_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("ws_retry_counter")
        .labelNames("ws_service")
        .help("Counts the amount of times this WS had to retry")
        .register()
