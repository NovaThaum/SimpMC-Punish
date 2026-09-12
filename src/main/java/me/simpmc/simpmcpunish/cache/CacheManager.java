package me.simpmc.simpmcpunish.cache;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.simpmc.simpmcpunish.SimpMCPunish;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.simpmc.simpmcpunish.model.Punishment;

public class CacheManager {
    private final Cache<UUID, Optional<Punishment>> banCache;
    private final Cache<UUID, Optional<Punishment>> muteCache;
    private final Cache<String, Optional<Punishment>> ipBanCache;
    private final Cache<String, Optional<Punishment>> ipMuteCache;

    public CacheManager(SimpMCPunish plugin) {
        int cacheExpiry = plugin.getConfig().getInt("cache.expiry-minutes", 10);
        int muteCacheExpiry = Math.max(1, plugin.getConfig().getInt("cache.mute-expiry-seconds", 5));
        int maxSize = plugin.getConfig().getInt("cache.max-size", 1000);
        this.banCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(cacheExpiry, TimeUnit.MINUTES).recordStats().build();
        this.muteCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(muteCacheExpiry, TimeUnit.SECONDS).recordStats().build();
        this.ipBanCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(cacheExpiry, TimeUnit.MINUTES).recordStats().build();
        this.ipMuteCache = Caffeine.newBuilder().maximumSize(maxSize).expireAfterWrite(muteCacheExpiry, TimeUnit.SECONDS).recordStats().build();
        plugin.getLogger().info("缓存管理器已初始化（最大数量: " + maxSize + "，处罚缓存过期时间: " + cacheExpiry + " 分钟，禁言缓存同步周期: " + muteCacheExpiry + " 秒）");
    }

    public Optional<Punishment> getActiveBan(UUID uuid) {
        Optional<Punishment> cached = this.banCache.getIfPresent(uuid);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                this.banCache.invalidate(uuid);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }

    public Optional<Punishment> getActiveMute(UUID uuid) {
        Optional<Punishment> cached = this.muteCache.getIfPresent(uuid);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                this.muteCache.invalidate(uuid);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }

    public void cacheBan(UUID uuid, Optional<Punishment> punishment) {
        this.banCache.put(uuid, punishment);
    }

    public void cacheMute(UUID uuid, Optional<Punishment> punishment) {
        this.muteCache.put(uuid, punishment);
    }

    public Optional<Punishment> getActiveIpBan(String ipAddress) {
        Optional<Punishment> cached = this.ipBanCache.getIfPresent(ipAddress);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                this.ipBanCache.invalidate(ipAddress);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }

    public Optional<Punishment> getActiveIpMute(String ipAddress) {
        Optional<Punishment> cached = this.ipMuteCache.getIfPresent(ipAddress);
        if (cached != null) {
            if (cached.isPresent() && cached.get().isExpired()) {
                this.ipMuteCache.invalidate(ipAddress);
                return Optional.empty();
            }
            return cached;
        }
        return null;
    }

    public void cacheIpBan(String ipAddress, Optional<Punishment> punishment) {
        this.ipBanCache.put(ipAddress, punishment);
    }

    public void cacheIpMute(String ipAddress, Optional<Punishment> punishment) {
        this.ipMuteCache.put(ipAddress, punishment);
    }

    public void invalidate(UUID uuid) {
        this.banCache.invalidate(uuid);
        this.muteCache.invalidate(uuid);
    }

    public void invalidateBan(UUID uuid) {
        this.banCache.invalidate(uuid);
    }

    public void invalidateMute(UUID uuid) {
        this.muteCache.invalidate(uuid);
    }

    public void invalidateIpBan(String ipAddress) {
        this.ipBanCache.invalidate(ipAddress);
    }

    public void invalidateIpMute(String ipAddress) {
        this.ipMuteCache.invalidate(ipAddress);
    }

    public void clear() {
        this.banCache.invalidateAll();
        this.muteCache.invalidateAll();
        this.ipBanCache.invalidateAll();
        this.ipMuteCache.invalidateAll();
    }

    public String getStats() {
        return String.format("封禁缓存: %d | 禁言缓存: %d | IP 封禁缓存: %d | IP 禁言缓存: %d", this.banCache.estimatedSize(), this.muteCache.estimatedSize(), this.ipBanCache.estimatedSize(), this.ipMuteCache.estimatedSize());
    }

    public void cleanup() {
        this.banCache.cleanUp();
        this.muteCache.cleanUp();
        this.ipBanCache.cleanUp();
        this.ipMuteCache.cleanUp();
    }
}


