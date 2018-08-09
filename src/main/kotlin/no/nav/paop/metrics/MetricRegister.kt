package no.nav.paop.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

const val METRICS_NS = "paop"

val RULE_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("rule_counter")
        .labelNames("rule_name")
        .help("Counts the number of times this rule is used").register()

val WS_CALL_TIME: Summary = Summary.Builder()
        .namespace(METRICS_NS)
        .name("ws_call_time")
        .labelNames("service")
        .help("Time it takes to execute a soap call").register()

val REQUEST_TIME: Summary = Summary.build()
        .namespace(METRICS_NS)
        .name("request_time_ms")
        .help("Request time in milliseconds.").register()

val MESSAGE_OUTCOME_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("message_outcome_counter")
        .labelNames("message_outcome_type")
        .help("Counts the number of messages that gets a outcome type").register()

val INCOMING_MESSAGE_COUNTER: Counter = Counter.build()
        .namespace(METRICS_NS)
        .name("incoming_message_count")
        .help("Counts the number of incoming messages")
        .register()

val RETRY_COUNTER: Counter = Counter.Builder()
        .namespace(METRICS_NS)
        .name("ws_retry_counter")
        .labelNames("ws_service")
        .help("Counts the amount of times this WS had to retry")
        .register()
