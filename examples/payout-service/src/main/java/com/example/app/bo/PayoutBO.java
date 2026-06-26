package com.example.app.bo;

import java.sql.Connection;

import com.example.app.cache.PayoutCache;
import com.example.app.code.PayoutStatusType;
import com.example.app.dao.PayoutDAO;
import com.example.app.dto.Payout;
import com.example.app.provider.PayoutProviderManager;
import com.example.app.util.ServerRoleUtils;

/**
 * CPS-06 · BO 業務聚合層：private 建構子 + 全靜態。持有交易邊界（連線由 BO 取得/關閉）。
 * 依賴只往下：BO → Cache / DAO / DTO（+ provider、util 橫切）。
 */
public final class PayoutBO {

    /** 連線來源；實務上 wire 到連線池，範例預設未接。 */
    private static volatile ConnectionProvider connections = () -> {
        throw new UnsupportedOperationException("wire a real connection pool");
    };

    private PayoutBO() {
        throw new AssertionError("no instance");
    }

    public static void wireConnections(ConnectionProvider provider) {
        connections = provider;
    }

    /** 讀路徑：cache-first，miss 再回 DAO 並回填。 */
    public static Payout getPayout(long id) {
        Payout cached = PayoutCache.getInstance().get(id);
        if (cached != null) {
            return cached;
        }
        try (Connection conn = connections.get()) {
            Payout p = PayoutDAO.findById(conn, id);
            if (p != null) {
                PayoutCache.getInstance().put(p);
            }
            return p;
        } catch (Exception e) {
            throw new RuntimeException("getPayout failed: " + id, e);
        }
    }

    /** 寫路徑：只在 TRANSACTION 角色 server 處理（CPS-08 gate）。 */
    public static long requestPayout(long customerId, long amount) {
        if (!ServerRoleUtils.isTransactionServer()) {
            throw new IllegalStateException("requestPayout only runs on TRANSACTION server");
        }
        try (Connection conn = connections.get()) {
            long id = PayoutDAO.insertRequested(conn, customerId, amount, PayoutStatusType.REQUESTED.getValue());
            String txnRef = PayoutProviderManager.getInstance()
                    .send(new Payout(id, customerId, amount, PayoutStatusType.REQUESTED.getValue(), false, null));
            PayoutDAO.updateStatus(conn, id, PayoutStatusType.SENT.getValue());
            PayoutCache.getInstance().evict(id);
            return txnRef == null ? -1L : id;
        } catch (Exception e) {
            throw new RuntimeException("requestPayout failed", e);
        }
    }

    /** 連線來源合約（避免 BO 直接耦合特定連線池）。 */
    public interface ConnectionProvider {
        Connection get() throws Exception;
    }
}
