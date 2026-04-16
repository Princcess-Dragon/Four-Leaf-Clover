package com.clover.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 验证码VO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaVO implements Serializable {

    /**
     * 验证码唯一标识
     */
    private String captchaKey;

    /**
     * 验证码图片（Base64编码）
     */
    private String captchaImage;
}
