package com.csy.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;

public class CsyApplicationContext {
    private Class configClass;

    private ConcurrentHashMap<String,Object> beanMap = new ConcurrentHashMap<>();//存储所有bean
    private HashMap<String,BeanDefinition> beanDefinitionMap = new HashMap<>();//存储bean的定义

    public CsyApplicationContext(Class appconfig) {
        this.configClass = appconfig;

        //Step1.进行扫描
        if(appconfig.isAnnotationPresent(ComponentScan.class)){  //判断是否是ComponentScan注解
            ComponentScan componentScan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScan.value();  //获取扫描的路径

            path = path.replace(".", "/");
            ClassLoader classLoader = CsyApplicationContext.class.getClassLoader();
            try {
                //获取扫描的包下面的所有文件
                URL url = classLoader.getResource(path);

                File file = new File((url).getFile());
                if(file.isDirectory()) {
                    File[] files = file.listFiles();
                    for (File f : files) {
                        if (f.getName().endsWith(".class")) { //只扫描class文件
                            String absolutePath = f.getAbsolutePath();
                            String className = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                            className = className.replace("/", ".");
                            Class clazz = classLoader.loadClass(className);
                            if (clazz.isAnnotationPresent(Component.class)) { //判断该对象是否包含Component注解
                                System.out.println("扫描到bean：" + clazz);
                                Component component = (Component) clazz.getAnnotation(Component.class);
                                String beanName = component.value();//获取bean的名称

                                if(beanName.equals("")){
                                    beanName = clazz.getSimpleName().substring(0,1).toLowerCase() + clazz.getSimpleName().substring(1);
                                }

                                Scope scope = (Scope) clazz.getAnnotation(Scope.class);
                                if(scope != null){
                                    beanDefinitionMap.put(beanName, new BeanDefinition(clazz, beanName, scope.value()));
                                }else{
                                    beanDefinitionMap.put(beanName, new BeanDefinition(clazz, beanName, "singleton"));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Step2.创建所有单例对象
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if(beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanDefinition);
                beanMap.put(beanName, bean);
            }
        }
    }



    private synchronized Object createBean(BeanDefinition beanDefinition) {
        try {
            Constructor<?> constructor = beanDefinition.getBeanClass().getConstructor(); //获取构造函数
            Object bean = constructor.newInstance(); //创建bean实例

            //注入
            for (Field field : beanDefinition.getBeanClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(bean, getBean(field.getName()));
                }
            }
            return bean;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to instantiate bean: " + beanDefinition.getBeanName(), e);
        }
    }


    /**
     *
     * @param name
     * @return 对应的bean示例
     */
    public Object getBean(String name) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(name);
        Class<?> beanClass = beanDefinition.getBeanClass();
        if (beanClass == null) {
            return null;
        }
        if ("singleton".equals(beanDefinition.getScope())) {
            Object bean = beanMap.get(beanDefinition.getBeanName());
            if(bean == null){
                bean = createBean(beanDefinition);
                beanMap.put(beanDefinition.getBeanName(), bean);
                return bean;
            }else{
                return bean;
            }
        }else {
            return createBean(beanDefinition);
        }
    }
}
