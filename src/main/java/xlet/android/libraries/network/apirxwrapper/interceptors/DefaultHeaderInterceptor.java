package xlet.android.libraries.network.apirxwrapper.interceptors;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import xlet.android.libraries.network.apirxwrapper.ApiUrlChecker;

public abstract class DefaultHeaderInterceptor implements Interceptor, ApiUrlChecker {
    private final ApiUrlChecker apiUrlChecker;

    public DefaultHeaderInterceptor() {
        this(null);
    }

    public DefaultHeaderInterceptor(@Nullable ApiUrlChecker apiUrlChecker) {
        this.apiUrlChecker = apiUrlChecker;
    }

    @Override
    public boolean needAllDefaultHeader(String method, String targetUrl) {
        if (apiUrlChecker != null) {
            return apiUrlChecker.needAllDefaultHeader(method, targetUrl);
        } else {
            return true;
        }
    }

    @Nullable
    public abstract HashMap<String, String> getDefaultHeaderMap();


    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        HashMap<String, String> defaultHeader = getDefaultHeaderMap();
        Request origin = chain.request();
        if (defaultHeader != null && !defaultHeader.isEmpty()) {
            boolean needDefault = needAllDefaultHeader(origin.method(), origin.url().toString());
            if (needDefault) {
                Request.Builder newRequestBuilder = origin.newBuilder();
                Headers originHeaders = origin.headers();
                for (Map.Entry<String, String> entry : defaultHeader.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    boolean isHeaderKeyEmpty = originHeaders.get(key) == null;
                    if (isHeaderKeyEmpty) {
                        // Only add when has no exist header value
                        newRequestBuilder.addHeader(key, value);
                    }
                }
                return chain.proceed(newRequestBuilder.build());
            }
        }
        return chain.proceed(origin);
    }
}
