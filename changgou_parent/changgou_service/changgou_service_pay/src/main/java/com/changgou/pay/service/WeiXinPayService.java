package com.changgou.pay.service;

import java.util.Map;

public interface WeiXinPayService {

    // 创建二维码
    Map creteNative(Map<String, String> paraMap);

    // 查询微信支付状态
    Map queryStatus(String outradeno);
}
