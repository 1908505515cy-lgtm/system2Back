package com.example.controller;

import com.example.common.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.exception.CustomException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/test")
public class TestController {
    @GetMapping("/hello")
    public Result hello(){
//        int i= 1/0;
//        return Result.success(123);
        throw new CustomException(ResultCodeEnum.PARAM_ERROR);
    }
}
