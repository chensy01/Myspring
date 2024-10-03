package com.csy.service;

import com.csy.spring.Component;
import com.csy.spring.CsyApplicationContext;

@Component
public class Main {
    public static void main(String[] args) {
        CsyApplicationContext context = new CsyApplicationContext(AppConfig.class);

        UserService userService = (UserService) context.getBean("userService"); //从spring中获取对象（标记了Component的对象）

        userService.test();
    }
}