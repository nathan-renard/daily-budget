package com.aimbire.dailybudget;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Base64;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.BillingResponseCode;
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pay-what-you-want Pro: three one-time products at different prices, any of which
 * unlocks the widget. Google Play's billing goes through the Play Store app on the
 * device, so this needs no server and no INTERNET permission.
 *
 * The unlock is cached in SharedPreferences for the widget, and re-checked against
 * Play on every resume, so a refunded purchase locks Pro again.
 */
final class Pro {

    /** Product IDs as created in Play Console (one-time products), cheapest first. */
    static final String[] PRODUCTS = {"pro_small", "pro_medium", "pro_large"};

    /**
     * Base64 RSA public key from Play Console (Monetize with Play → Monetization setup →
     * Licensing). Purchases whose signature doesn't match are ignored. Left empty, the
     * check is skipped, which is fine for testing but should be filled in for release.
     */
    private static final String PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApWa5Ddw12lWzMzmc+TxhnTLcBzi7GDCh8tPJap6+W3Jw7smtCO9gOw"
            + "MD1yxGPkrLM9UbUMIAQPx7nS+3q7RUFIVvcam/NcWYH7VU0UCj+Yec5JJg280+8+w/tFnzTwy+GgHxxndKCr1POdp/kQKgIFEP"
            + "r0mvPecSRCmN+WK9HcpZfQAaKCrj8VWJy2jBNyP0oM8QO6keRs7QdMF68fXUi6DLrNrCAq3KBnG5S4Ac1717cNKOO+vD+YbnKz"
            + "LDC6bfWY+uHtK5qnUdZ2qfr2REzlpURHcCLHdWgGzGnhrOOw5APSB8uuO99/yedVaB/TWPrCqe4YKggeqptSaCU8MLnQIDAQAB";

    private static final String PREFS = "pro";
    private static final String TAG = "DailyBudget";

    /** Receives the state JSON for the web UI whenever something changes. */
    interface Listener {
        void onProState(String json);
    }

    private final Activity activity;
    private final Listener listener;
    private final BillingClient client;
    private final Map<String, ProductDetails> details = new HashMap<>();
    private final Set<String> owned = new HashSet<>();
    private String status = "connecting";   // connecting | ready | unavailable
    private boolean pending;

    Pro(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        client = BillingClient.newBuilder(activity)
                .setListener(this::onPurchasesUpdated)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
        connect();
    }

    /**
     * Auto-reconnection only covers a lost connection, not a failed setup, so refresh()
     * retries this: Play can become usable later (signed in, Play Store updated).
     */
    private void connect() {
        status = "connecting"; // not published: a retry keeps the last answer on screen
        client.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                if (result.getResponseCode() == BillingResponseCode.OK) {
                    status = "ready";
                    refresh(); // also loads the prices
                } else {
                    Log.d(TAG, "Billing unavailable: " + result.getDebugMessage());
                    status = "unavailable";
                    publish();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() reconnects on the next call.
            }
        });
    }

    // ---- Cached state (readable without a billing connection) ---------------

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isPro(Context context) {
        return prefs(context).getBoolean("pro", false)
                || (isDebuggable(context) && prefs(context).getBoolean("debug_pro", false));
    }

    private static boolean isDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /** Debug builds only: flips Pro without a purchase, to test the widget. */
    void toggleDebugPro() {
        if (!isDebuggable(activity)) return;
        prefs(activity).edit().putBoolean("debug_pro", !prefs(activity).getBoolean("debug_pro", false)).apply();
        BudgetWidget.updateAll(activity);
        publish();
    }

    // ---- Billing --------------------------------------------------------------

    private void queryProducts() {
        List<QueryProductDetailsParams.Product> products = new ArrayList<>();
        for (String id : PRODUCTS) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(ProductType.INAPP)
                    .build());
        }
        client.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(products).build(),
                (result, productResult) -> {
                    if (result.getResponseCode() == BillingResponseCode.OK) {
                        for (ProductDetails pd : productResult.getProductDetailsList()) {
                            details.put(pd.getProductId(), pd);
                        }
                        if (!productResult.getUnfetchedProductList().isEmpty()) {
                            Log.d(TAG, "Products not found in Play Console: "
                                    + productResult.getUnfetchedProductList().size());
                        }
                    }
                    publish();
                });
    }

    /**
     * Re-reads what the user owns. Play answers from its cache, so this works offline.
     * Runs when the app resumes and when the Pro sheet opens, and retries what failed before.
     */
    void refresh() {
        if (!"ready".equals(status)) {
            if ("unavailable".equals(status)) connect(); // which refreshes once it's ready
            return;
        }
        if (details.isEmpty()) queryProducts(); // none loaded yet, or that query failed
        client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build(),
                (result, purchases) -> {
                    if (result.getResponseCode() != BillingResponseCode.OK) return; // keep the cached state
                    owned.clear();
                    pending = false;
                    handle(purchases, true);
                });
    }

    void buy(String productId) {
        ProductDetails pd = details.get(productId);
        if (pd == null) return;
        BillingFlowParams.ProductDetailsParams.Builder product =
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd);
        List<ProductDetails.OneTimePurchaseOfferDetails> offers = pd.getOneTimePurchaseOfferDetailsList();
        if (offers != null && !offers.isEmpty()) product.setOfferToken(offers.get(0).getOfferToken());
        BillingFlowParams params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(product.build()))
                .build();
        activity.runOnUiThread(() -> client.launchBillingFlow(activity, params));
    }

    private void onPurchasesUpdated(BillingResult result, List<Purchase> purchases) {
        if (result.getResponseCode() == BillingResponseCode.OK && purchases != null) {
            handle(purchases, false);
        } else if (result.getResponseCode() == BillingResponseCode.ITEM_ALREADY_OWNED) {
            refresh();
        }
    }

    /**
     * Grants Pro for completed purchases and acknowledges them (Play refunds them otherwise).
     * {@code complete} means this is everything the user owns, so an empty list revokes Pro;
     * a single new purchase can only add to it.
     */
    private void handle(List<Purchase> purchases, boolean complete) {
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                pending = true;
            } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && verify(purchase)) {
                owned.addAll(purchase.getProducts());
                if (!purchase.isAcknowledged()) {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken()).build(), r -> { });
                }
            }
        }
        boolean wasPro = isPro(activity);
        if (complete || !owned.isEmpty()) prefs(activity).edit().putBoolean("pro", !owned.isEmpty()).apply();
        if (isPro(activity) != wasPro) BudgetWidget.updateAll(activity);
        publish();
    }

    private static boolean verify(Purchase purchase) {
        if (PUBLIC_KEY.isEmpty()) return true;
        try {
            PublicKey key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.DEFAULT)));
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initVerify(key);
            signature.update(purchase.getOriginalJson().getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.decode(purchase.getSignature(), Base64.DEFAULT));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            Log.w(TAG, "Purchase signature check failed", e);
            return false;
        }
    }

    // ---- State for the web UI --------------------------------------------------

    /** {pro, status, pending, debug, products: [{id, price, owned}]} */
    String state() {
        try {
            JSONArray products = new JSONArray();
            for (String id : PRODUCTS) {
                ProductDetails pd = details.get(id);
                ProductDetails.OneTimePurchaseOfferDetails offer = pd == null ? null : pd.getOneTimePurchaseOfferDetails();
                if (offer == null) continue;
                products.put(new JSONObject()
                        .put("id", id)
                        .put("price", offer.getFormattedPrice())
                        .put("owned", owned.contains(id)));
            }
            return new JSONObject()
                    .put("pro", isPro(activity))
                    .put("status", status)
                    .put("pending", pending)
                    .put("debug", isDebuggable(activity))
                    .put("products", products)
                    .toString();
        } catch (JSONException e) {
            return "{}";
        }
    }

    private void publish() {
        String json = state();
        activity.runOnUiThread(() -> listener.onProState(json));
    }

    void end() {
        client.endConnection();
    }
}
