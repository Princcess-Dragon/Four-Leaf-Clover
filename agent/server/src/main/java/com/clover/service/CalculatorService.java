package com.clover.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算器服务
 */
@Slf4j
@Service
public class CalculatorService {

    @PostConstruct
    public void init() {
        log.info("计算器服务初始化完成");
    }

    /**
     * 处理计算请求
     */
    public String handleCalculationRequest(String message) {
        try {
            // 提取数学表达式
            String expression = extractExpression(message);
            if (expression == null || expression.isEmpty()) {
                return "请提供要计算的数学表达式，例如：计算 2 + 3 * 4";
            }

            log.info("计算表达式: {}", expression);

            // 计算结果
            double result = evaluate(expression);

            // 格式化输出
            return String.format("🔢 计算结果：\n\n%s = %.2f", expression, result);

        } catch (Exception e) {
            log.error("计算失败", e);
            return "计算失败：" + e.getMessage();
        }
    }

    /**
     * 提取数学表达式
     */
    private String extractExpression(String message) {
        // 移除"计算"、"等于"等关键词
        String cleaned = message.replaceAll(".*(计算|等于|等于多少|是多少)\\s*", "").trim();
        
        // 如果清理后还是原消息，尝试直接查找数学表达式
        if (cleaned.equals(message)) {
            Pattern pattern = Pattern.compile("[\\d+\\-*/().\\s]+");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                cleaned = matcher.group().trim();
            }
        }

        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * 简单的数学表达式求值（支持 + - * / 和括号）
     */
    private double evaluate(String expression) {
        try {
            // 验证表达式只包含合法字符
            if (!expression.matches("^[\\d+\\-*/().\\s]+$")) {
                throw new IllegalArgumentException("表达式包含非法字符");
            }
            
            // 使用递归下降解析器计算表达式
            ExpressionParser parser = new ExpressionParser(expression);
            return parser.parse();
        } catch (Exception e) {
            throw new RuntimeException("无法计算表达式: " + expression, e);
        }
    }
    
    /**
     * 简单的数学表达式解析器（支持加减乘除和括号）
     */
    private static class ExpressionParser {
        private final String expression;
        private int pos = 0;
        
        public ExpressionParser(String expression) {
            this.expression = expression.replaceAll("\\s+", ""); // 移除空格
        }
        
        public double parse() {
            double result = parseExpression();
            if (pos < expression.length()) {
                throw new IllegalArgumentException("表达式格式错误，位置: " + pos);
            }
            return result;
        }
        
        // 处理加减法（最低优先级）
        private double parseExpression() {
            double result = parseTerm();
            
            while (pos < expression.length()) {
                char op = expression.charAt(pos);
                if (op == '+' || op == '-') {
                    pos++;
                    double right = parseTerm();
                    if (op == '+') {
                        result += right;
                    } else {
                        result -= right;
                    }
                } else {
                    break;
                }
            }
            
            return result;
        }
        
        // 处理乘除法（中等优先级）
        private double parseTerm() {
            double result = parseFactor();
            
            while (pos < expression.length()) {
                char op = expression.charAt(pos);
                if (op == '*' || op == '/') {
                    pos++;
                    double right = parseFactor();
                    if (op == '*') {
                        result *= right;
                    } else {
                        if (right == 0) {
                            throw new ArithmeticException("除数不能为零");
                        }
                        result /= right;
                    }
                } else {
                    break;
                }
            }
            
            return result;
        }
        
        // 处理数字、负号和括号（最高优先级）
        private double parseFactor() {
            // 处理一元负号
            if (pos < expression.length() && expression.charAt(pos) == '-') {
                pos++;
                return -parseFactor();
            }
            
            // 处理正号
            if (pos < expression.length() && expression.charAt(pos) == '+') {
                pos++;
                return parseFactor();
            }
            
            // 处理括号
            if (pos < expression.length() && expression.charAt(pos) == '(') {
                pos++; // 跳过 '('
                double result = parseExpression();
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new IllegalArgumentException("缺少右括号");
                }
                pos++; // 跳过 ')'
                return result;
            }
            
            // 处理数字
            int start = pos;
            while (pos < expression.length() && 
                   (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                pos++;
            }
            
            if (start == pos) {
                throw new IllegalArgumentException("期望数字，但找到: " + 
                    (pos < expression.length() ? expression.charAt(pos) : "EOF"));
            }
            
            return Double.parseDouble(expression.substring(start, pos));
        }
    }
}
