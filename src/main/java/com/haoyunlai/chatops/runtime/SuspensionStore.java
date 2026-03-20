package com.haoyunlai.chatops.runtime;

public interface SuspensionStore {

    void save(String token, SuspensionContext context);

    SuspensionContext remove(String token);
}
