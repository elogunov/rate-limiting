package com.github.elo.rate.limiting;

import java.time.LocalDateTime;

public class LimiterResponse<T> {
    final private T partner;
    final private boolean accept;
    final private LocalDateTime requestDateTime;

    public LimiterResponse(boolean accept, T partner, LocalDateTime requestDateTime) {
        this.partner = partner;
        this.accept = accept;
        this.requestDateTime = requestDateTime;
    }

    public boolean isAccept() {
        return accept;
    }

    public LocalDateTime getRequestDateTime() {
        return requestDateTime;
    }

    public T getPartner() {
        return partner;
    }

    @Override
    public String toString() {
        return "LimiterResponse{" + "partner=" + partner +
                ", accept=" + accept +
                ", requestDateTime=" + requestDateTime +
                '}';
    }
}
