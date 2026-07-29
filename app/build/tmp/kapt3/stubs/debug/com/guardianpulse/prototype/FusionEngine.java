package com.guardianpulse.prototype;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0010\u0007\n\u0002\b\n\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\u0015H\u0002J\u001e\u0010\u0016\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u0014\u001a\u00020\u0015J&\u0010\u0018\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0019\u001a\u00020\u00072\u0006\u0010\u001a\u001a\u00020\u00072\u0006\u0010\u0014\u001a\u00020\u0015J\u0006\u0010\u001b\u001a\u00020\u001cR \u0010\u0003\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00070\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u00020\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR\u001a\u0010\u000e\u001a\u00020\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u000b\"\u0004\b\u0010\u0010\r\u00a8\u0006\u001d"}, d2 = {"Lcom/guardianpulse/prototype/FusionEngine;", "", "()V", "audioHistory", "", "Lkotlin/Pair;", "", "", "hrHistory", "lastAudioFlagTimestamp", "getLastAudioFlagTimestamp", "()J", "setLastAudioFlagTimestamp", "(J)V", "lastHrFlagTimestamp", "getLastHrFlagTimestamp", "setLastHrFlagTimestamp", "evaluateFusion", "", "timestamp", "settings", "Lcom/guardianpulse/prototype/ThresholdSettings;", "processAudio", "db", "processHR", "hr", "baselineHR", "reset", "", "app_debug"})
public final class FusionEngine {
    @org.jetbrains.annotations.NotNull
    private final java.util.List<kotlin.Pair<java.lang.Long, java.lang.Float>> hrHistory = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<kotlin.Pair<java.lang.Long, java.lang.Float>> audioHistory = null;
    private long lastHrFlagTimestamp = 0L;
    private long lastAudioFlagTimestamp = 0L;
    
    public FusionEngine() {
        super();
    }
    
    public final long getLastHrFlagTimestamp() {
        return 0L;
    }
    
    public final void setLastHrFlagTimestamp(long p0) {
    }
    
    public final long getLastAudioFlagTimestamp() {
        return 0L;
    }
    
    public final void setLastAudioFlagTimestamp(long p0) {
    }
    
    public final boolean processHR(long timestamp, float hr, float baselineHR, @org.jetbrains.annotations.NotNull
    com.guardianpulse.prototype.ThresholdSettings settings) {
        return false;
    }
    
    public final boolean processAudio(long timestamp, float db, @org.jetbrains.annotations.NotNull
    com.guardianpulse.prototype.ThresholdSettings settings) {
        return false;
    }
    
    private final boolean evaluateFusion(long timestamp, com.guardianpulse.prototype.ThresholdSettings settings) {
        return false;
    }
    
    public final void reset() {
    }
}