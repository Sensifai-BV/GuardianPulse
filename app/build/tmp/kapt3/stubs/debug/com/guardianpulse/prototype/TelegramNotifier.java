package com.guardianpulse.prototype;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0012\u001a\u00020\u00042\u0006\u0010\u0013\u001a\u00020\u0004H\u0002J\u001c\u0010\u0014\u001a\u00020\u00112\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00110\u0016H\u0086@\u00a2\u0006\u0002\u0010\u0017J\u0016\u0010\u0018\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u001aH\u0086@\u00a2\u0006\u0002\u0010\u001bJ\u0016\u0010\u001c\u001a\u00020\u00112\u0006\u0010\u0019\u001a\u00020\u001aH\u0086@\u00a2\u0006\u0002\u0010\u001bJ\u000e\u0010\u001d\u001a\u00020\u0011H\u0086@\u00a2\u0006\u0002\u0010\u001eR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u00020\nX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000b\u0010\f\"\u0004\b\r\u0010\u000eR\u000e\u0010\u000f\u001a\u00020\u0004X\u0082D\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001f"}, d2 = {"Lcom/guardianpulse/prototype/TelegramNotifier;", "", "()V", "chatId1", "", "chatId2", "chatId3", "client", "Lokhttp3/OkHttpClient;", "lastUpdateId", "", "getLastUpdateId", "()J", "setLastUpdateId", "(J)V", "token", "answerCallbackQuery", "", "callbackQueryId", "text", "pollForAck", "onAckReceived", "Lkotlin/Function0;", "(Lkotlin/jvm/functions/Function0;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendAlert", "level", "", "(ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendLowBatteryAlert", "sendTamperAlert", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class TelegramNotifier {
    @org.jetbrains.annotations.NotNull
    private static final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String token = "8761093889:AAFkpGrJkpWwUsB4BqGk1idGzm8xI0_gTYM";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String chatId1 = "815242420";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String chatId2 = "815242420";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String chatId3 = "815242420";
    private static long lastUpdateId = 0L;
    @org.jetbrains.annotations.NotNull
    public static final com.guardianpulse.prototype.TelegramNotifier INSTANCE = null;
    
    private TelegramNotifier() {
        super();
    }
    
    public final long getLastUpdateId() {
        return 0L;
    }
    
    public final void setLastUpdateId(long p0) {
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object sendAlert(int level, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object sendTamperAlert(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object sendLowBatteryAlert(int level, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object pollForAck(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> onAckReceived, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void answerCallbackQuery(java.lang.String callbackQueryId, java.lang.String text) {
    }
}