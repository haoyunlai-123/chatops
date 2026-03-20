package com.haoyunlai.chatops.runtime;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemorySuspensionStore implements SuspensionStore {

    private final Map<String, SuspensionContext> suspended = new ConcurrentHashMap<>();

    @Override
    public void save(String token, SuspensionContext context) {
        suspended.put(token, context);
    }

    @Override
    public SuspensionContext remove(String token) {
        return suspended.remove(token);
    }
}
