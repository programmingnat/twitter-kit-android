package com.twitter.sdk.android.core;

import io.fabric.sdk.android.services.persistence.PreferenceStore;
import io.fabric.sdk.android.services.persistence.PreferenceStoreStrategy;
import io.fabric.sdk.android.services.persistence.SerializationStrategy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link com.twitter.sdk.android.core.SessionManager} that persists sessions.
 */
public class PersistedSessionManager<T extends Session> implements SessionManager<T> {

    private static final int NUM_SESSIONS = 1;

    private final PreferenceStore preferenceStore;
    private final SerializationStrategy<T> serializer;
    private final ConcurrentHashMap<Long, T> sessionMap;
    private final ConcurrentHashMap<Long, PreferenceStoreStrategy<T>> storageMap;
    private final PreferenceStoreStrategy<T> activeSessionStorage;
    private final AtomicReference<T> activeSessionRef;
    private final String prefKeyActiveSession;
    private final String prefKeySession;
    private volatile boolean restorePending = true;

    public PersistedSessionManager(PreferenceStore preferenceStore,
            SerializationStrategy<T> serializer, String prefKeyActiveSession,
            String prefKeySession) {
        this(preferenceStore, serializer, new ConcurrentHashMap<Long, T>(NUM_SESSIONS),
                new ConcurrentHashMap<Long, PreferenceStoreStrategy<T>>(NUM_SESSIONS),
                new PreferenceStoreStrategy<T>(preferenceStore, serializer,
                        prefKeyActiveSession), prefKeyActiveSession, prefKeySession);
    }

    PersistedSessionManager(PreferenceStore preferenceStore,
            SerializationStrategy<T> serializer, ConcurrentHashMap<Long, T> sessionMap,
            ConcurrentHashMap<Long, PreferenceStoreStrategy<T>> storageMap,
            PreferenceStoreStrategy<T> activesSessionStorage, String prefKeyActiveSession,
            String prefKeySession) {
        this.preferenceStore = preferenceStore;
        this.serializer = serializer;
        this.sessionMap = sessionMap;
        this.storageMap = storageMap;
        this.activeSessionStorage = activesSessionStorage;
        this.activeSessionRef = new AtomicReference<>();
        this.prefKeyActiveSession = prefKeyActiveSession;
        this.prefKeySession = prefKeySession;
    }

    void restoreAllSessionsIfNecessary() {
        // Only restore once
        if (restorePending) {
            restoreAllSessions();
        }
    }

    private synchronized void restoreAllSessions() {
        if (restorePending) {
            restoreActiveSession();
            restoreSessions();
            restorePending = false;
        }
    }

    private void restoreSessions() {
        T session;

        final Map<String, ?> preferences = preferenceStore.get().getAll();
        for (Map.Entry<String, ?> entry : preferences.entrySet()) {
            if (isSessionPreferenceKey(entry.getKey())) {
                session = serializer.deserialize((String) entry.getValue());
                if (session != null) {
                    internalSetSession(session.getId(), session, false);
                }
            }
        }

    }

    private void restoreActiveSession() {
        final T session = activeSessionStorage.restore();
        if (session != null) {
            internalSetSession(session.getId(), session, false);
        }
    }

    boolean isSessionPreferenceKey(String preferenceKey) {
        return preferenceKey.startsWith(prefKeySession);
    }

    /**
     * @return the active session, may return {@code null} if there's no session.
     */
    @Override
    public T getActiveSession() {
        restoreAllSessionsIfNecessary();
        return activeSessionRef.get();
    }

    /**
     * Sets the active session.
     */
    @Override
    public void setActiveSession(T session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null!");
        }
        restoreAllSessionsIfNecessary();
        internalSetSession(session.getId(), session, true);
    }

    /**
     * Clears the active session.
     */
    @Override
    public void clearActiveSession() {
        restoreAllSessionsIfNecessary();
        if (activeSessionRef.get() != null) {
            clearSession(activeSessionRef.get().getId());
        }
    }

    /**
     * @return the session associated with the id, may return {@code null} if there's no session.
     */
    @Override
    public T getSession(long id) {
        restoreAllSessionsIfNecessary();
        return sessionMap.get(id);
    }

    /**
     * Sets the session to associate with the id. If there is no active session, this session also
     * becomes the active session.
     */
    @Override
    public void setSession(long id, T session) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null!");
        }
        restoreAllSessionsIfNecessary();
        internalSetSession(id, session, false);
    }

    @Override
    public Map<Long, T> getSessionMap() {
        restoreAllSessionsIfNecessary();
        return Collections.unmodifiableMap(sessionMap);
    }

    private void internalSetSession(long id, T session, boolean forceUpdate) {
        sessionMap.put(id, session);
        PreferenceStoreStrategy<T> storage = storageMap.get(id);
        if (storage == null) {
            storage = new PreferenceStoreStrategy<T>(preferenceStore, serializer, getPrefKey(id));
            storageMap.putIfAbsent(id, storage);
        }
        storage.save(session);

        final T activeSession = activeSessionRef.get();
        if (activeSession == null || forceUpdate) {
            synchronized (this) {
                activeSessionRef.compareAndSet(activeSession, session);
                activeSessionStorage.save(session);
            }
        }
    }

    String getPrefKey(long id) {
        return prefKeySession + "_" + id;
    }

    /**
     * Clears the session associated with the id.
     */
    @Override
    public void clearSession(long id) {
        restoreAllSessionsIfNecessary();
        if (activeSessionRef.get() != null && activeSessionRef.get().getId() == id) {
            synchronized (this) {
                activeSessionRef.set(null);
                activeSessionStorage.clear();
            }
        }

        sessionMap.remove(id);
        final PreferenceStoreStrategy<T> storage = storageMap.remove(id);
        if (storage != null) {
            storage.clear();
        }
    }
}
