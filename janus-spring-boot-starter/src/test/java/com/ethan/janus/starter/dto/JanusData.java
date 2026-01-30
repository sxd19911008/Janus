package com.ethan.janus.starter.dto;

import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class JanusData {

    private AtomicInteger count;
    private LocalDateTime primaryStartDate;
    private LocalDateTime primaryEndDate;
    private LocalDateTime secondaryStartDate;
    private LocalDateTime secondaryEndDate;
    private StopWatch primaryStopWatch;
    private StopWatch secondaryStopWatch;

    public AtomicInteger getCount() {
        return count;
    }

    public void setCount(AtomicInteger count) {
        this.count = count;
    }

    public LocalDateTime getPrimaryStartDate() {
        return primaryStartDate;
    }

    public void setPrimaryStartDate(LocalDateTime primaryStartDate) {
        this.primaryStartDate = primaryStartDate;
    }

    public LocalDateTime getPrimaryEndDate() {
        return primaryEndDate;
    }

    public void setPrimaryEndDate(LocalDateTime primaryEndDate) {
        this.primaryEndDate = primaryEndDate;
    }

    public LocalDateTime getSecondaryStartDate() {
        return secondaryStartDate;
    }

    public void setSecondaryStartDate(LocalDateTime secondaryStartDate) {
        this.secondaryStartDate = secondaryStartDate;
    }

    public LocalDateTime getSecondaryEndDate() {
        return secondaryEndDate;
    }

    public void setSecondaryEndDate(LocalDateTime secondaryEndDate) {
        this.secondaryEndDate = secondaryEndDate;
    }

    public StopWatch getPrimaryStopWatch() {
        return primaryStopWatch;
    }

    public void setPrimaryStopWatch(StopWatch primaryStopWatch) {
        this.primaryStopWatch = primaryStopWatch;
    }

    public StopWatch getSecondaryStopWatch() {
        return secondaryStopWatch;
    }

    public void setSecondaryStopWatch(StopWatch secondaryStopWatch) {
        this.secondaryStopWatch = secondaryStopWatch;
    }
}
