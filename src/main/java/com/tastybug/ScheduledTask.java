package com.tastybug;

public class ScheduledTask implements Runnable {
    @Override
    public void run() {
        System.out.println("hello world");
    }
}