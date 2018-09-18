package com.github.elo.rate.limiting.algo;

import com.github.elo.rate.limiting.LimiterResponse;

public interface RateLimiter<T> {

    LimiterResponse<T> accept(T partnerId);
}
