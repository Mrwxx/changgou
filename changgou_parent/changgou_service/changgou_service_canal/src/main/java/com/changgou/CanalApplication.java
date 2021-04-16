package com.changgou;

import com.xpand.starter.canal.annotation.EnableCanalClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import javax.imageio.IIOException;
import java.io.IOException;

// 不自动启动DataSource数据源
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
@EnableEurekaClient
@EnableCanalClient
@EnableFeignClients(basePackages = {"com.changgou.content.feign", "com.changgou.item.feign"})
public class CanalApplication {

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            SpringApplication.run(CanalApplication.class,args);
        } catch (Throwable e) {
            // 为什么我将 Exception e 换成了 Throwable e它就不会直接执行e.printStackTrace()了呢？
            // 可能是因为Throwable是error和exception的基类，能够与Error匹配，而Exception无法匹配
            // block
            System.in.read();
            e.printStackTrace();
        }

    }
}
