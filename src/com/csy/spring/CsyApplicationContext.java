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
                // 这里应该是一个目录，如果返回false，可能有以下几个原因：
                // 1. 文件路径不正确
                // 2. 没有足够的权限访问该目录
                // 3. 文件系统出现问题
                // 为了调试，我们可以添加一些日志输出
                String projectRoot = System.getProperty("user.dir");
                System.out.println("Project root: " + projectRoot);
                System.out.println("File path: " + file.getAbsolutePath());
                System.out.println("Is directory: " + file.isDirectory());
                System.out.println("Exists: " + file.exists());
                System.out.println("Can read: " + file.canRead());
                
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

            //实现自动注入，利用反射获取类上所有属性，判断是否有Autowired注解，如果有则进行自动注入
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
                bean = createBean(beanDefinition);//这里非常关键，如果缓存中没有就创建一个，并放入缓存中
                beanMap.put(beanDefinition.getBeanName(), bean);
            }
            return bean;
        }else {
            return createBean(beanDefinition);//如果是prototype，就直接创建一个，这里可能会存在递归生成的情况，因为循环依赖的问题
        }
    }
}
