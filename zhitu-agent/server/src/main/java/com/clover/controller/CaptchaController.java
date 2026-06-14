package com.clover.controller;

import com.clover.result.Result;
import com.clover.utils.CaptchaCodeUtil;
import com.clover.vo.CaptchaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 验证码控制器
 */
@RestController
@RequestMapping("/api/captcha")
@Slf4j
public class CaptchaController {

    /**
     * 获取图片验证码
     */
    @GetMapping("/image")
    public Result<CaptchaVO> getCaptcha() {
        // 生成唯一标识
        String captchaKey = UUID.randomUUID().toString().replace("-", "");

        // 生成验证码图片
        String captchaImage = CaptchaCodeUtil.generateCaptchaImage(captchaKey);

        log.info("生成图片验证码: captchaKey={}", captchaKey);

        CaptchaVO captchaVO = new CaptchaVO(captchaKey, captchaImage);
        return Result.success(captchaVO);
    }
}
