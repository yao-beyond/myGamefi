package com.example.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;

import com.example.app.cache.PayoutCache;
import com.example.app.code.PayoutProviderType;
import com.example.app.code.PayoutStatusType;
import com.example.app.code.ServerRole;
import com.example.app.dao.InMemoryPayoutSource;
import com.example.app.dto.Payout;
import com.example.app.provider.PayoutProviderManager;
import com.example.app.util.ServerRoleUtils;

/** 範例的行為驗證（CPS-01/02/03/08 的 acceptance）。 */
public class PayoutDomainTest {

    @Before
    public void reset() {
        PayoutCache.getInstance().resetForTest();
        PayoutProviderManager.getInstance().resetForTest();
    }

    // CPS-02：enum roundtrip + 述詞
    @Test
    public void typeEnum_roundtrip_and_predicates() {
        for (PayoutStatusType t : PayoutStatusType.values()) {
            assertEquals(t, PayoutStatusType.getInstanceOf(t.getValue()).get());
        }
        assertTrue(PayoutStatusType.REQUESTED.canCancel());
        assertTrue(PayoutStatusType.CONFIRMED.isTerminal());
        assertFalse(PayoutStatusType.getInstanceOf(999).isPresent());
    }

    // CPS-01：熱切換（init-then-swap）+ 委派
    @Test
    public void providerManager_hotSwap() {
        PayoutProviderManager mgr = PayoutProviderManager.getInstance();
        mgr.configureSettingSource(() -> PayoutProviderType.BANK_WIRE.getValue());
        mgr.reload();
        Payout p = new Payout(1, 10, 500, PayoutStatusType.REQUESTED.getValue(), false, null);
        assertTrue(mgr.send(p).startsWith("bankwire-"));

        mgr.configureSettingSource(() -> PayoutProviderType.EWALLET.getValue());
        mgr.reload();
        assertTrue(mgr.send(p).startsWith("ewallet-"));
    }

    // CPS-03：時間戳增量更新 + tombstone + 冪等
    @Test
    public void cache_incrementalUpdate_and_tombstone() {
        InMemoryPayoutSource source = new InMemoryPayoutSource();
        PayoutCache cache = PayoutCache.getInstance();
        cache.wire(source);

        source.upsert(new Payout(1, 10, 500, 1, false, new Timestamp(2000)));
        cache.update();
        assertEquals(1, cache.size());
        assertEquals(500, cache.get(1).getAmount());

        // 冪等：重複更新同一筆不增量
        cache.update();
        assertEquals(1, cache.size());

        // tombstone：刪除標記應從 cache 移除
        source.upsert(new Payout(1, 10, 500, 4, true, new Timestamp(5000)));
        cache.update();
        assertNull(cache.get(1));
    }

    // CPS-08：bitmask 複合角色 + 成員資格
    @Test
    public void serverRole_bitmask_composite() {
        int mask = ServerRole.mask(ServerRole.CUSTOMER, ServerRole.CACHE);
        assertEquals(1 | 32, mask);
        assertTrue(ServerRoleUtils.has(mask, ServerRole.CUSTOMER));
        assertTrue(ServerRoleUtils.has(mask, ServerRole.CACHE));
        assertFalse(ServerRoleUtils.has(mask, ServerRole.TRANSACTION));
    }
}
