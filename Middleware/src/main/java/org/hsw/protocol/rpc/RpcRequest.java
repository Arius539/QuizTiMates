package org.hsw.protocol.rpc;

import org.hsw.protocol.message.MethodId;

import java.util.List;

public record RpcRequest(MethodId method, List<String> content) {

    public RpcRequest {
        if (method == null) {
            throw new IllegalArgumentException("MethodId darf nicht null sein.");
        }

        content = content == null ? List.of() : List.copyOf(content);
    }
}