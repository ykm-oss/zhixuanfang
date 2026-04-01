package com.shop.common.exception;

import com.shop.common.constant.ResultCodeEnum;
import lombok.Getter;

/**
 * 业务异常类
 */
@Getter
public class BusinessException extends RuntimeException{

    private final Integer code;
    private final String message;

    public BusinessException(ResultCodeEnum codeEnum){
        super(codeEnum.getMessage());
        this.code=codeEnum.getCode();
        this.message= codeEnum.getMessage();
    }

    public BusinessException(Integer code,String message){
        super(message);
        this.code=code;
        this.message=message;
    }
}
