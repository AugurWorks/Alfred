package com.augurworks.alfred;

import lombok.Data;

@Data
public class TimingInfo {

    private final long startTime;
    private final long maxAllowedDuration;

    public static TimingInfo withDuration(long maxAllowedDuration) {
        return new TimingInfo(System.currentTimeMillis(), maxAllowedDuration);
    }

    public boolean hasTimeExpired() {
        if (maxAllowedDuration <= 0) {
            return false;
        } else {
            return System.currentTimeMillis() > (startTime + maxAllowedDuration);
        }
    }
}
