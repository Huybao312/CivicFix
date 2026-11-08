package com.civicfix.backend.accounts;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RequestLimits {
  private record Bucket(long until, int count) {}

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Clock clock;

  public RequestLimits(Clock clock) {
    this.clock = clock;
  }

  public synchronized boolean take(String key, int limit, long seconds) {
    long now = clock.millis();
    if (buckets.size() > 10000) buckets.entrySet().removeIf(e -> e.getValue().until() < now);
    if (buckets.size() > 20000 && !buckets.containsKey(key)) return false;
    Bucket b = buckets.get(key);
    if (b == null || b.until() <= now) b = new Bucket(now + seconds * 1000, 0);
    if (b.count() >= limit) return false;
    buckets.put(key, new Bucket(b.until(), b.count() + 1));
    return true;
  }

  public void clear() {
    buckets.clear();
  }
}
