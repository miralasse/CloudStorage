package com.cloud.common;

public class CmdMessage extends Message {
    private String text;

    public CmdMessage(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
