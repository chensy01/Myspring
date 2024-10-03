package com.csy.spring;

import java.util.HashMap;

public class BeanDefinition {
    private Class beanClass;
    private String beanName;
    private String scope;


    public String getBeanName() {
        return beanName;
    }

    public BeanDefinition(Class beanClass, String beanName, String scope) {
        this.beanClass = beanClass;
        this.beanName = beanName;
        this.scope = scope;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }



    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class beanClass) {
        this.beanClass = beanClass;
    }
}
