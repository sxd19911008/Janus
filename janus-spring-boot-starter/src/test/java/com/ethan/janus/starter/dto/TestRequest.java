package com.ethan.janus.starter.dto;


public class TestRequest {

    private String key;

    public TestRequest(String key) {
        this.key = key;
    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
