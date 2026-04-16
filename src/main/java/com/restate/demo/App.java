package com.restate.demo;

import dev.restate.sdk.endpoint.Endpoint;
import dev.restate.sdk.http.vertx.RestateHttpServer;

public class App {
    public static void main(String[] args) {
        RestateHttpServer.listen(
                Endpoint.bind(new PaymentAuth())
                        .bind(new ClearingGateway())
                        .bind(new ClearingWorkflow())
                        .bind(new MerchantSettlement())
                        .bind(new SettlementWindow()),
                9080);
    }
}
