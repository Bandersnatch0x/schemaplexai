package com.schemaplexai.common.observability;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;
import java.util.List;

public class PiiRedactingSpanExporter implements SpanExporter {


    private final SpanExporter delegate;

    public PiiRedactingSpanExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public io.opentelemetry.sdk.common.CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> redacted = spans.stream()
            .map(s -> (SpanData) new PiiRedactedSpanData(s))
            .toList();
        return delegate.export(redacted);
    }

    @Override
    public io.opentelemetry.sdk.common.CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public io.opentelemetry.sdk.common.CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    static String redact(String input) {
        return PiiRedactor.redact(input);
    }

    private static final class PiiRedactedSpanData extends DelegatingSpanData {

        private final Attributes redactedAttributes;

        PiiRedactedSpanData(SpanData delegate) {
            super(delegate);
            this.redactedAttributes = redactAttributes(delegate.getAttributes());
        }

        @Override
        public Attributes getAttributes() {
            return redactedAttributes;
        }

        private static Attributes redactAttributes(Attributes attributes) {
            AttributesBuilder builder = Attributes.builder();
            attributes.forEach((key, value) -> {
                if (value instanceof String str) {
                    builder.put(AttributeKey.stringKey(key.getKey()), redact(str));
                } else {
                    builder.put((AttributeKey<Object>) key, value);
                }
            });
            return builder.build();
        }
    }
}
