package com.github.elo.rate.limiting.algo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

class FixedWindowTest extends RateLimiterTest {

    @BeforeEach
    void setUp() {
    }

    @Override
    protected RateLimiter<String> getInstance() {
        return new FixedWindow<>(100, Duration.ofSeconds(10));
    }

    @Override
    @RepeatedTest(50)
    void acceptAll() throws ExecutionException, InterruptedException {
        super.acceptAll();
    }

    @Override
    @RepeatedTest(50)
    void acceptFirst100() throws ExecutionException, InterruptedException {
        super.acceptFirst100();
    }

    @Override
    @RepeatedTest(5)
    void worstCaseForFixedWindow() throws Exception {
        int acceptedCounter = getAcceptedCounter();
        Assertions.assertEquals(199, acceptedCounter);
    }

    @AfterEach
    void tearDown() {
    }
}