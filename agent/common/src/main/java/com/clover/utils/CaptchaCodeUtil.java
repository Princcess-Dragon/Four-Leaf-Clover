package com.clover.utils;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 验证码工具类
 */
@Slf4j
public class CaptchaCodeUtil {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final int CODE_LENGTH = 4;
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /**
     * 生成验证码图片（Base64编码）
     *
     * @param captchaKey 验证码唯一标识
     * @return Base64编码的验证码图片
     */
    public static String generateCaptchaImage(String captchaKey) {
        try {
            // 创建验证码图片
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();

            // 设置背景色
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // 设置字体
            Font font = new Font("Arial", Font.BOLD, 24);
            g2d.setFont(font);

            // 生成随机验证码
            Random random = new Random();
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = random.nextInt(CHARACTERS.length());
                char c = CHARACTERS.charAt(index);
                code.append(c);

                // 随机颜色
                g2d.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));

                // 绘制字符
                int x = 20 + i * 25;
                int y = 28 + random.nextInt(5);
                g2d.drawString(String.valueOf(c), x, y);
            }

            // 添加干扰线
            for (int i = 0; i < 5; i++) {
                g2d.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g2d.drawLine(x1, y1, x2, y2);
            }

            g2d.dispose();

            // 转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(bytes);

            log.debug("生成验证码: key={}, code={}", captchaKey, code.toString());

            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            log.error("生成验证码失败", e);
            throw new RuntimeException("生成验证码失败", e);
        }
    }
}
