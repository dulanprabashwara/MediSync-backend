package com.medisync.notification.dto;

public class UnreadNotificationCountDto {
    private long count;

    public UnreadNotificationCountDto() {}

    public UnreadNotificationCountDto(long count) {
        this.count = count;
    }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
