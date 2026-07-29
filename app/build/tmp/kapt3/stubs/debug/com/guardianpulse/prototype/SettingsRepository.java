package com.guardianpulse.prototype;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\u0007\u001a\u00020\bJ\r\u0010\t\u001a\u0004\u0018\u00010\n\u00a2\u0006\u0002\u0010\u000bJ\r\u0010\f\u001a\u0004\u0018\u00010\n\u00a2\u0006\u0002\u0010\u000bJ\u0006\u0010\r\u001a\u00020\u000eJ\u000e\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u000eJ\u000e\u0010\u0011\u001a\u00020\b2\u0006\u0010\u0012\u001a\u00020\nJ\u000e\u0010\u0013\u001a\u00020\b2\u0006\u0010\u0012\u001a\u00020\nR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/guardianpulse/prototype/SettingsRepository;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "prefs", "Landroid/content/SharedPreferences;", "clearBaselines", "", "getBaselineAudio", "", "()Ljava/lang/Float;", "getBaselineHR", "getThresholdSettings", "Lcom/guardianpulse/prototype/ThresholdSettings;", "saveThresholdSettings", "settings", "setBaselineAudio", "value", "setBaselineHR", "app_debug"})
public final class SettingsRepository {
    @org.jetbrains.annotations.NotNull
    private final android.content.SharedPreferences prefs = null;
    
    public SettingsRepository(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float getBaselineHR() {
        return null;
    }
    
    public final void setBaselineHR(float value) {
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Float getBaselineAudio() {
        return null;
    }
    
    public final void setBaselineAudio(float value) {
    }
    
    public final void clearBaselines() {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.guardianpulse.prototype.ThresholdSettings getThresholdSettings() {
        return null;
    }
    
    public final void saveThresholdSettings(@org.jetbrains.annotations.NotNull
    com.guardianpulse.prototype.ThresholdSettings settings) {
    }
}