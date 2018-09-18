package com.github.elo.rate.limiting.algo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

class SlidingLogTest extends RateLimiterTest {

    @BeforeEach
    void setUp() {
    }

    @Override
    protected RateLimiter<String> getInstance() {
        return new SlidingLog<>(100, Duration.ofSeconds(10));
    }

    @Override
    @RepeatedTest(50)
    void acceptFirst100() throws ExecutionException, InterruptedException {
        super.acceptFirst100();
    }

    @Override
    @RepeatedTest(5)
    void worstCaseForFixedWindow() throws Exception {
        super.worstCaseForFixedWindow();
    }

    @AfterEach
    void tearDown() {
    }
}