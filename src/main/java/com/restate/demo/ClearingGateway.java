package com.restate.demo;

import com.restate.demo.types.ClearingEvent;
import com.restate.demo.types.ClearingFile;
import dev.restate.sdk.Context;
import dev.restate.sdk.annotation.Handler;
import dev.restate.sdk.annotation.Service;

@Service
public class ClearingGateway {

    @Handler
    public String ingest(Context ctx, ClearingFile file) {
        System.out.println("[ClearingGateway] Ingesting clearing file with " + file.events().size() + " events");

        for (ClearingEvent event : file.events()) {
            ClearingWorkflowClient.fromContext(ctx, event.paymentId())
                    .send()
                    .run(event);
            System.out.println("[ClearingGateway] Dispatched " + event.paymentId());
        }

        return "Dispatched " + file.events().size() + " clearing events";
    }
}
