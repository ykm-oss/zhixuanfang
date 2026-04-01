package com.shop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@SpringBootApplication
@MapperScan("com.shop.mapper")
@EnableCaching      // 开启缓存
@EnableScheduling   // 开启定时任务
public class ShopApplication {
    public static void main(String[] args) {

        SpringApplication.run(ShopApplication.class, args);
    }
}