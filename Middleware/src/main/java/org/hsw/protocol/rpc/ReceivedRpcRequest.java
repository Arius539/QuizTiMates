package org.hsw.protocol.rpc;

public record ReceivedRpcRequest(long callId, RpcRequest request) {
}
