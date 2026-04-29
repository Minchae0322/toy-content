package com.example.toycontent.support.fixture;

import com.example.toycontent.app.carrier.domain.Carrier;

public class CarrierFixture {

    public static final Long DEFAULT_CARRIER_ID = 100L;
    public static final Long DEFAULT_USER_ID = 1L;

    private CarrierFixture() {}

    public static Carrier defaultCarrier() {
        return Carrier.builder()
                .id(DEFAULT_CARRIER_ID)
                .userId(DEFAULT_USER_ID)
                .build();
    }
}
