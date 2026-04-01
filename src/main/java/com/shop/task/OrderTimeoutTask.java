package com.shop.task;


import com.shop.service.OrderInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时定时任务
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderInfoService orderInfoService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void closeTimeoutOrders(){
        log.info("定时任务触发：开始关闭超时订单");
        try{
            orderInfoService.closeTimeoutOrder();
        }catch (Exception e){
            log.info("定时任务执行失败：{}", e.getMessage(), e);
        }
    }
}
