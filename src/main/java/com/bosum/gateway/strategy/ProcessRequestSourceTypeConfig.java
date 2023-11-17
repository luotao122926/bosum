package com.bosum.gateway.strategy;


import com.bosum.gateway.enums.ProcessTypeEnumFlag;
import com.bosum.gateway.enums.RequestSourceEnum;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;


/**
 *  @author zuhao.ouyang
 */
@Component
public class ProcessRequestSourceTypeConfig implements ApplicationContextAware, InitializingBean  {

    public static Map<RequestSourceEnum, Strategy> generatorStrategyMap;

    private ApplicationContext applicationContext;

    private ProcessRequestSourceTypeConfig() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initGeneratorStrategyMap();
    }

    private void initGeneratorStrategyMap() {
        //获取所有关于Strategy的bean
        Map<String, Strategy> beanMap = applicationContext.getBeansOfType(Strategy.class);
        //策略枚举与具体策略实现关联(将标注了@ProcessTypeEnumFlag注解的类和枚举做关联)
        Map<RequestSourceEnum, Strategy> result = EnumBeanMapUtil.beanMap2EnumMap(beanMap, ProcessTypeEnumFlag.class,
                ProcessTypeEnumFlag::value);
        setGeneratorStrategyMap(result);
    }

    /**
     * @param generatorStrategyMap key枚举，value对应的实现
     */
    private static void setGeneratorStrategyMap(Map<RequestSourceEnum, Strategy> generatorStrategyMap) {
        ProcessRequestSourceTypeConfig.generatorStrategyMap = generatorStrategyMap;
    }
}
