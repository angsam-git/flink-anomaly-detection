package com.angsam.flinkanomalydetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class AsyncModelPredictFunction
    extends RichAsyncFunction<LogEntry, LogWithAnomaly> {

    private transient OkHttpClient client;
    private transient ObjectMapper mapper;
    private final String modelUrl;
    private static final MediaType JSON =
        MediaType.get("application/json; charset=utf-8");

    public AsyncModelPredictFunction(String modelUrl) {
        this.modelUrl = modelUrl;
    }

    @Override
    public void open(OpenContext openContext) {
        client = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build();
        mapper = new ObjectMapper();
    }

    @Override
    public void asyncInvoke(LogEntry entry,
                        ResultFuture<LogWithAnomaly> resultFuture) {
        try {
            byte[] payload = mapper.writeValueAsBytes(entry);
            Request httpReq = new Request.Builder()
                .url(modelUrl)
                .post(RequestBody.create(payload, JSON))
                .build();

            client.newCall(httpReq).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    resultFuture.complete(Collections.emptyList());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody body = response.body()) {
                        if (!response.isSuccessful() || body == null) {
                            resultFuture.complete(Collections.emptyList());
                            return;
                        }
                        PredictResponse pr = mapper.readValue(
                            body.string(), PredictResponse.class
                        );
                        resultFuture.complete(
                            Collections.singletonList(
                                new LogWithAnomaly(
                                    entry,
                                    pr.reconstruction_error,
                                    pr.is_anomaly
                                )
                            )
                        );
                    } catch (Exception ex) {
                        resultFuture.complete(Collections.emptyList());
                    }
                }
            });
        } catch (IOException e) {
            resultFuture.complete(Collections.emptyList());
        }
    }

}
