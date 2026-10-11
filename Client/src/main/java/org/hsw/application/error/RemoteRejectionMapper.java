package org.hsw.application.error;

import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.message.ErrorCode;

import java.util.Objects;

public final class RemoteRejectionMapper {
    private RemoteRejectionMapper() {
    }

    public static ServiceErrorKind map(ErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode");
        return switch (errorCode) {
            case NONE -> ServiceErrorKind.PROTOCOL_ERROR;
            case INVALID_ARGUMENT -> ServiceErrorKind.VALIDATION;
            case INVALID_STATE -> ServiceErrorKind.INVALID_STATE;
            case NOT_IMPLEMENTED -> ServiceErrorKind.FEATURE_UNAVAILABLE;
            case INTERNAL_ERROR -> ServiceErrorKind.SERVER_ERROR;
            case CALLBACK_UNAVAILABLE -> ServiceErrorKind.CONNECTION;
        };
    }

    public static ServiceException serviceExceptionFromRemote(RemoteActionException rejection) {
        Objects.requireNonNull(rejection, "rejection");
        return new ServiceException(map(rejection.code()), rejection);
    }

    public static void throwServiceExceptionFromRemote(RemoteActionException rejection) {
        throw serviceExceptionFromRemote(rejection);
    }
}
