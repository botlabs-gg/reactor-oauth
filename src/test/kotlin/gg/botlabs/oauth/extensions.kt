package gg.botlabs.oauth

import org.junit.jupiter.api.fail
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

fun <T> Publisher<T>.verifier(): StepVerifier.FirstStep<T> = StepVerifier.create(this)

fun <T> Mono<T>.expectCompleteEmpty(): Duration = doOnEach {
    if (it.hasValue()) {
        fail("Expected empty, bot got ${it.get()}.")
    }
}.verifier().verifyComplete()