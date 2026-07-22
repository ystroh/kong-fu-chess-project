package com.chessgame.common.protocol.response;

public record ErrorMessage(ErrorCode code, String detail) {
}