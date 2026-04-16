package com.course.newsplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.course.newsplatform.mapper")
@SpringBootApplication
public class NewsPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsPlatformApplication.class, args);
    }
}
