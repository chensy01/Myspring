package com.csy.service;

import com.csy.spring.Autowired;
import com.csy.spring.Component;
import com.csy.spring.Scope;

@Component
@Scope("singleton")
public class UserService {
    @Autowired
    private OrderService orderService;

    public void test()
    {
        System.out.println("orderService " + orderService);
    }
}
