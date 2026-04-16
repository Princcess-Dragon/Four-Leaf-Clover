package com.clover.config;

import com.clover.interceptor.JwtTokenUserInterceptor;
import com.clover.json.JacksonObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     *
     * @param registry
     */
    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login")
                .excludePathPatterns("/api/user/register")
                .excludePathPatterns("/api/captcha/**")
                .excludePathPatterns("/api/video/list")
                .excludePathPatterns("/api/video/{id}")
                .excludePathPatterns("/api/video/partition/**")
                .excludePathPatterns("/api/video/user/**")
                .excludePathPatterns("/api/danmu/list/**")
                .excludePathPatterns("/api/comment/list/**");
    }

    /**
     * 通过knife4j生成接口文档 (OpenAPI 3.0)
     * @return
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("B站后端接口文档")
                        .version("1.0")
                        .description("B站后端接口文档")
                        .contact(new Contact().name("clover")));
    }

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    /**
     * 设置静态资源映射
     * @param registry
     */
    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("配置静态资源映射...");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        // 配置 pages 目录下的 HTML 文件访问
        registry.addResourceHandler("/pages/**")
                .addResourceLocations("classpath:/pages/");
        // 配置 page 目录下的 HTML 文件访问（兼容旧路径）
        registry.addResourceHandler("/page/**")
                .addResourceLocations("classpath:/pages/");
        // 配置根路径下的 HTML 文件访问（用于 Controller 直接返回 HTML 文件名）
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/pages/");
        // 配置静态资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:D:/code/java/bilibili/clover-server/src/main/resources/upload/");
    }
    /**
     * 扩展Spring MVC框架的消息转换器
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.info("扩展消息转换器...");
        //创建一个消息转换器对象
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        //需要为消息转换器设置一个对象转换器，对象转换器可以将Java对象序列化为json数据
        converter.setObjectMapper(new JacksonObjectMapper());
        //将自己的消息转换器加入容器中
        converters.add(0, converter);
    }

    /**
     * 配置CORS跨域请求
     * @param registry
     */
    @Override
    protected void addCorsMappings(CorsRegistry registry) {
        log.info("配置CORS跨域请求...");
        registry.addMapping("/api/**")
                .allowedOriginPatterns("")  // 允许所有来源，生产环境建议指定具体域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

}
