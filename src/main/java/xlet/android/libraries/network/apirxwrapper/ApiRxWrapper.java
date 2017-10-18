package xlet.android.libraries.network.apirxwrapper;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiRxWrapper {
    private Retrofit retrofitInstance;
    private ExecutorService networkThreadPool;
    private Scheduler networkScheduler;

    //private final static int MAX_NETWORK_THREAD_SIZE = 3;
    //private ExecutorService parallelThreadPool;
    //private Scheduler parallelScheduler;


    private ApiRxWrapper(final Gson gson, final OkHttpClient okHttpClient, final String baseUrl) {
        this.retrofitInstance = generateRetrofitClient(gson, okHttpClient, baseUrl);
        this.networkThreadPool = Executors.newSingleThreadExecutor();
        this.networkScheduler = Schedulers.from(networkThreadPool);

        //this.parallelThreadPool = Executors.newFixedThreadPool(MAX_NETWORK_THREAD_SIZE);
        //this.parallelScheduler = Schedulers.from(parallelThreadPool);
    }

    @NonNull
    private Retrofit generateRetrofitClient(final Gson gson, final OkHttpClient okHttpClient, final String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    public <T> T getApiService(final Class<T> service) {
        return retrofitInstance.create(service);
    }

    public <T> Observable<T> executeAsObservable(Call<T> call) {
        return new ExecuteCallObservable<>(call);
    }

    public <T> Completable executeAsCompletable(Call<T> call) {
        return executeAsObservable(call).ignoreElements();
    }

    public <T> Single<T> executeAsSingle(Call<T> call) {
        return executeAsObservable(call).singleOrError();
    }

    public <T> Maybe<T> executeAsMaybe(Call<T> call) {
        return executeAsObservable(call).singleElement();
    }

    public <T> Flowable<T> executeAsFlowable(Call<T> call) {
        return executeAsObservable(call).toFlowable(BackpressureStrategy.LATEST);
    }

    /**
     * Use {@link #getNetworkScheduler()}
     *
     * @return
     */
    @Deprecated
    public ExecutorService getNetworkThreadPool() {
        return networkThreadPool;
    }

    ///**
    // * Use {@link #getParallelScheduler()}
    // *
    // * @return
    // */
    //@Deprecated
    //public ExecutorService getParallelThreadPool() {
    //    return parallelThreadPool;
    //}

    public Scheduler getNetworkScheduler() {
        return networkScheduler;
    }

    //public Scheduler getParallelScheduler() {
    //    return parallelScheduler;
    //}

    public Scheduler getMainScheduler() {
        return AndroidSchedulers.mainThread();
    }

    public static class Builder {
        private Context context;
        private String baseUrl;
        private Gson gson;
        private List<Interceptor> customInterceptorList = null;

        public Builder(Context context) {
            this.context = context.getApplicationContext();
        }

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setGson(Gson gson) {
            this.gson = gson;
            return this;
        }

        public Builder addCustomInterceptor(Interceptor interceptor) {
            if (customInterceptorList == null) {
                customInterceptorList = new ArrayList<>();
            }
            if (interceptor != null) {
                customInterceptorList.add(interceptor);
            }
            return this;
        }

        @NonNull
        private OkHttpClient generateOkHttpClient() {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(30, TimeUnit.SECONDS);

            if (customInterceptorList != null && !customInterceptorList.isEmpty()) {
                for (Interceptor interceptor : customInterceptorList) {
                    builder.addInterceptor(interceptor);
                }
            }

            if (BuildConfig.DEBUG) {
                builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            }

            ConnectionSpec.Builder specBuilder = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                specBuilder.tlsVersions(TlsVersion.TLS_1_2)
                        .cipherSuites(
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256);
            } else {
                // It should Larger than Build.VERSION_CODES.HONEYCOMB (SDK 11)
                specBuilder.tlsVersions(TlsVersion.TLS_1_0)
                        .cipherSuites(
                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                                CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA);
            }
            builder.connectionSpecs(Arrays.asList(specBuilder.build(), ConnectionSpec.CLEARTEXT));
            return builder.build();
        }

        public ApiRxWrapper build() {
            OkHttpClient client = generateOkHttpClient();
            if (gson == null) {
                gson = new Gson();
            }
            return new ApiRxWrapper(gson, client, baseUrl);
        }
    }
}
