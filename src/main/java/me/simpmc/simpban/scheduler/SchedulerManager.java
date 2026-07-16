/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Entity
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package me.simpmc.simpban.scheduler;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class SchedulerManager {
    private final FoliaLib foliaLib;
    private final boolean isFolia;

    public SchedulerManager(JavaPlugin plugin) {
        this.foliaLib = new FoliaLib((Plugin)plugin);
        this.isFolia = this.foliaLib.isFolia();
    }

    public void runAsync(Runnable task) {
        this.foliaLib.getScheduler().runAsync(SchedulerManager.adapt(task));
    }

    public void runSync(Runnable task) {
        this.foliaLib.getScheduler().runNextTick(SchedulerManager.adapt(task));
    }

    public void runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        this.foliaLib.getScheduler().runLaterAsync(SchedulerManager.adapt(task), delay, unit);
    }

    public void runSyncLater(Runnable task, long delay, TimeUnit unit) {
        this.foliaLib.getScheduler().runLater(SchedulerManager.adapt(task), delay, unit);
    }

    public void runAsyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        this.foliaLib.getScheduler().runTimerAsync(SchedulerManager.adapt(task), initialDelay, period, unit);
    }

    public void runSyncRepeating(Runnable task, long initialDelay, long period, TimeUnit unit) {
        this.foliaLib.getScheduler().runTimer(SchedulerManager.adapt(task), initialDelay, period, unit);
    }

    public void runForEntity(Entity entity, Runnable task) {
        this.foliaLib.getScheduler().runAtEntity(entity, SchedulerManager.adapt(task));
    }

    public void runForEntityLater(Entity entity, Runnable task, long delay, TimeUnit unit) {
        this.foliaLib.getScheduler().runAtEntityLater(entity, SchedulerManager.adapt(task), delay, unit);
    }

    public boolean isFolia() {
        return this.isFolia;
    }

    public void shutdown() {
    }

    private static Consumer<WrappedTask> adapt(Runnable task) {
        return wrappedTask -> task.run();
    }
}


