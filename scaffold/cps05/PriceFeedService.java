package com.example.scaffold.cps05;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * CPS-05 · 單執行緒 WebSocket 廣播（對應 myGameFi 的 MarketPriceService）。
 *
 * 不變式：
 *  - @ServerEndpoint；連線集合用 ConcurrentHashMap<id, Session>。
 *  - 廣播用「單一」newSingleThreadExecutor（無鎖、訊息有序）。
 *  - 所有 outbound 經同一 executor；推送間隔固定（PUSH_INTERVAL_MS）。
 *  - 推送 thread 內禁止跑 DAO / blocking sync（只讀已備好的快照）。
 *  - send 失敗即移除該連線。
 */
@ServerEndpoint("/feed/price")
public final class PriceFeedService {

    private static final long PUSH_INTERVAL_MS = 250L;

    /** 全 endpoint 共享一個連線集合與一條廣播執行緒。 */
    private static final Map<String, Session> CONNECTIONS = new ConcurrentHashMap<>();

    private static final ExecutorService BROADCASTER =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "price-feed-broadcaster");
                t.setDaemon(true);
                return t;
            });

    private static volatile boolean running = false;

    /** 由 ServletContextListener 啟動一次。 */
    public static synchronized void start(PriceSnapshotSource source) {
        if (running) {
            return;
        }
        running = true;
        BROADCASTER.submit(() -> {
            while (running) {
                String payload = source.latestSnapshotJson();   // 只讀快照，不打 DAO
                broadcast(payload);
                sleep(PUSH_INTERVAL_MS);
            }
        });
    }

    public static synchronized void stop() {
        running = false;
    }

    @OnOpen
    public void onOpen(Session session) {
        CONNECTIONS.put(session.getId(), session);
    }

    @OnClose
    public void onClose(Session session) {
        CONNECTIONS.remove(session.getId());
    }

    @OnError
    public void onError(Session session, Throwable t) {
        CONNECTIONS.remove(session.getId());
    }

    /** 在單一廣播執行緒內呼叫，順序與無鎖由單執行緒保證。 */
    private static void broadcast(String payload) {
        for (Map.Entry<String, Session> e : CONNECTIONS.entrySet()) {
            Session session = e.getValue();
            try {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(payload);
                } else {
                    CONNECTIONS.remove(e.getKey());
                }
            } catch (IOException ex) {
                CONNECTIONS.remove(e.getKey());   // send 失敗即移除
            }
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }

    /** 推送資料來源：回傳已備好的快照（由 Cache 層產生），不在推送 thread 查 DB。 */
    public interface PriceSnapshotSource {
        String latestSnapshotJson();
    }
}
