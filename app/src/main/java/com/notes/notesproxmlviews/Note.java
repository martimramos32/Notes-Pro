package com.notes.notesproxmlviews;

import com.google.firebase.Timestamp;

public class Note {
    String title;
    String content;
    Timestamp timestamp;
    String imageBase64;
    int pinOrder;       // 1 = fixada no topo, 0 = normal
    long reminderTimestamp; // epoch ms; 0 = sem lembrete

    public Note() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public int getPinOrder() {
        return pinOrder;
    }

    public void setPinOrder(int pinOrder) {
        this.pinOrder = pinOrder;
    }

    public long getReminderTimestamp() {
        return reminderTimestamp;
    }

    public void setReminderTimestamp(long reminderTimestamp) {
        this.reminderTimestamp = reminderTimestamp;
    }
}
