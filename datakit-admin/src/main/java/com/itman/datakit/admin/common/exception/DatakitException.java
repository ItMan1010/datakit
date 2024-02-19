package com.itman.datakit.admin.common.exception;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DatakitException extends Exception {
    private final String errCode;
    private final String errMsg;
}
