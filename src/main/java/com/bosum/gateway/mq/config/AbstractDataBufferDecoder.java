package com.bosum.gateway.mq.config;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;



@SuppressWarnings("deprecation")
public abstract class AbstractDataBufferDecoder<T> extends AbstractDecoder<T> {
    private int maxInMemorySize = 100 * 1024 * 1024;


    protected AbstractDataBufferDecoder(MimeType... supportedMimeTypes) {
        super(supportedMimeTypes);
    }


    /**
     * Configure a limit on the number of bytes that can be buffered whenever
     * the input stream needs to be aggregated. This can be a result of
     * decoding to a single {@code DataBuffer},
     * {@link java.nio.ByteBuffer ByteBuffer}, {@code byte[]},
     * {@link org.springframework.core.io.Resource Resource}, {@code String}, etc.
     * It can also occur when splitting the input stream, e.g. delimited text,
     * in which case the limit applies to data buffered between delimiters.
     * <p>By default this is set to 256K.
     * @param byteCount the max number of bytes to buffer, or -1 for unlimited
     * @since 5.1.11
     */
    public void setMaxInMemorySize(int byteCount) {
        this.maxInMemorySize = byteCount;
    }

    /**
     * Return the {@link #setMaxInMemorySize configured} byte count limit.
     * @since 5.1.11
     */
    public int getMaxInMemorySize() {
        return this.maxInMemorySize;
    }


    @Override
    public Flux<T> decode(Publisher<DataBuffer> input, ResolvableType elementType,
                          @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

        return Flux.from(input).map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
    }

    @Override
    public Mono<T> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
                                @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

        return DataBufferUtils.join(input, this.maxInMemorySize)
                .map(buffer -> decodeDataBuffer(buffer, elementType, mimeType, hints));
    }

    /**
     * How to decode a {@code DataBuffer} to the target element type.
     * @deprecated as of 5.2, please implement
     * {@link #decode(DataBuffer, ResolvableType, MimeType, Map)} instead
     */
    @Deprecated
    @Nullable
    protected T decodeDataBuffer(DataBuffer buffer, ResolvableType elementType,
                                 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

        return decode(buffer, elementType, mimeType, hints);
    }


}
