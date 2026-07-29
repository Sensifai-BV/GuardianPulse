package com.guardianpulse.prototype;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0018\u001a\u00020\u0019H\u0002J\b\u0010\u001a\u001a\u00020\u0019H\u0002J\b\u0010\u001b\u001a\u00020\u0019H\u0002J\u0006\u0010\u001c\u001a\u00020\u0019J\u0006\u0010\u001d\u001a\u00020\u0019R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u00020\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u00050\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u001a\u0010\u0010\u001a\u00020\u0007X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\t\"\u0004\b\u0012\u0010\u000bR\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0016\u001a\u00020\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lcom/guardianpulse/prototype/AlertEscalationManager;", "", "()V", "_currentState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/guardianpulse/prototype/AlertState;", "cooldownDelayMillis", "", "getCooldownDelayMillis", "()J", "setCooldownDelayMillis", "(J)V", "currentState", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentState", "()Lkotlinx/coroutines/flow/StateFlow;", "escalationDelayMillis", "getEscalationDelayMillis", "setEscalationDelayMillis", "escalationJob", "Lkotlinx/coroutines/Job;", "pollingJob", "scope", "Lkotlinx/coroutines/CoroutineScope;", "enterCooldown", "", "handleAck", "startPollingForAck", "triggerAlert", "triggerTamperAlert", "app_debug"})
public final class AlertEscalationManager {
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.MutableStateFlow<com.guardianpulse.prototype.AlertState> _currentState = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.StateFlow<com.guardianpulse.prototype.AlertState> currentState = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable
    private static kotlinx.coroutines.Job escalationJob;
    @org.jetbrains.annotations.Nullable
    private static kotlinx.coroutines.Job pollingJob;
    private static long escalationDelayMillis = 30000L;
    private static long cooldownDelayMillis = 60000L;
    @org.jetbrains.annotations.NotNull
    public static final com.guardianpulse.prototype.AlertEscalationManager INSTANCE = null;
    
    private AlertEscalationManager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.guardianpulse.prototype.AlertState> getCurrentState() {
        return null;
    }
    
    public final long getEscalationDelayMillis() {
        return 0L;
    }
    
    public final void setEscalationDelayMillis(long p0) {
    }
    
    public final long getCooldownDelayMillis() {
        return 0L;
    }
    
    public final void setCooldownDelayMillis(long p0) {
    }
    
    public final void triggerAlert() {
    }
    
    public final void triggerTamperAlert() {
    }
    
    private final void startPollingForAck() {
    }
    
    private final void handleAck() {
    }
    
    private final void enterCooldown() {
    }
}