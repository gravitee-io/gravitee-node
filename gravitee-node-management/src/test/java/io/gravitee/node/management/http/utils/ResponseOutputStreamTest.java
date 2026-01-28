/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.node.management.http.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResponseOutputStreamTest {

    @Mock
    private HttpServerResponse response;

    private ResponseOutputStream outputStream;

    @BeforeEach
    void init() {
        outputStream = new ResponseOutputStream(response);
    }

    @Test
    void should_write_single_byte() throws IOException {
        when(response.closed()).thenReturn(false);
        when(response.writeQueueFull()).thenReturn(false);
        when(response.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

        outputStream.write(65); // 'A'

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).write(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().toString()).isEqualTo("A");
    }

    @Test
    void should_write_byte_array() throws IOException {
        when(response.closed()).thenReturn(false);
        when(response.writeQueueFull()).thenReturn(false);
        when(response.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

        byte[] data = "Hello World".getBytes();
        outputStream.write(data, 0, data.length);

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).write(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().toString()).isEqualTo("Hello World");
    }

    @Test
    void should_write_partial_byte_array() throws IOException {
        when(response.closed()).thenReturn(false);
        when(response.writeQueueFull()).thenReturn(false);
        when(response.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

        byte[] data = "Hello World".getBytes();
        outputStream.write(data, 6, 5); // "World"

        ArgumentCaptor<Buffer> bufferCaptor = ArgumentCaptor.forClass(Buffer.class);
        verify(response).write(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().toString()).isEqualTo("World");
    }

    @Test
    void should_throw_exception_when_response_is_closed() {
        when(response.closed()).thenReturn(true);

        assertThatThrownBy(() -> outputStream.write("test".getBytes(), 0, 4))
            .isInstanceOf(IOException.class)
            .hasMessage("Response is closed");
    }

    @Test
    void should_wait_for_drain_when_write_queue_is_full() throws IOException {
        when(response.closed()).thenReturn(false);
        // First call returns true (queue full), subsequent calls return false
        when(response.writeQueueFull()).thenReturn(true, false);
        when(response.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

        // Capture the drain handler and invoke it immediately
        doAnswer(invocation -> {
                Handler<Void> handler = invocation.getArgument(0);
                // Simulate drain by calling the handler
                handler.handle(null);
                return response;
            })
            .when(response)
            .drainHandler(any());

        byte[] data = "test".getBytes();
        outputStream.write(data, 0, data.length);

        verify(response).drainHandler(any());
        verify(response).write(any(Buffer.class));
    }

    @Test
    void should_handle_multiple_writes() throws IOException {
        when(response.closed()).thenReturn(false);
        when(response.writeQueueFull()).thenReturn(false);
        when(response.write(any(Buffer.class))).thenReturn(Future.succeededFuture());

        outputStream.write("Hello ".getBytes(), 0, 6);
        outputStream.write("World".getBytes(), 0, 5);

        verify(response, times(2)).write(any(Buffer.class));
    }

    @Test
    void flush_should_be_noop() throws IOException {
        // flush should not throw and should not interact with response
        outputStream.flush();
        verifyNoInteractions(response);
    }

    @Test
    void close_should_be_noop() throws IOException {
        // close should not throw and should not interact with response
        outputStream.close();
        verifyNoInteractions(response);
    }
}
