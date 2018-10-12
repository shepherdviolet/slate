package sviolet.slate.common.x.conversion.beanutil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sviolet.thistle.x.common.thistlespi.ThistleSpi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static sviolet.slate.common.x.conversion.beanutil.SlateBeanUtils.LOG_ENABLED;

/**
 * <p>默认Bean参数类型转换器</p>
 *
 * @author S.Violet
 */
public class DefaultBeanConverter extends BeanConverter {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBeanConverter.class);
    private static final String LOG_PREFIX = "SlateBeanUtils | ";

    private Map<Class<?>, Map<Class<?>, PropMapper>> propMappers = new HashMap<>();

    public DefaultBeanConverter() {
        List<PropMapper> propMapperList = ThistleSpi.getLoader().loadPlugins(PropMapper.class);
        if (propMapperList == null) {
            return;
        }
        for (PropMapper mapper : propMapperList) {
            if (mapper.fromType() == null || mapper.fromType().length <= 0) {
                throw new RuntimeException("Invalid PropMapper " + mapper.getClass().getName() + ", fromType method return null or empty, you can ignore this plugin by ThistleSpi");
            }
            if (mapper.toType() == null || mapper.toType().length <= 0) {
                throw new RuntimeException("Invalid PropMapper " + mapper.getClass().getName() + ", toType method return null or empty, you can ignore this plugin by ThistleSpi");
            }
            for (Class<?> fromType : mapper.fromType()) {
                for (Class<?> toType : mapper.toType()) {
                    initMapper(mapper, fromType, toType);
                }
            }

        }
    }

    private void initMapper(PropMapper mapper, Class<?> fromType, Class<?> toType) {
        Map<Class<?>, PropMapper> mappers = propMappers.get(fromType);
        if (mappers == null) {
            mappers = new HashMap<>();
            propMappers.put(fromType, mappers);
        }
        //遇到同类型映射器时, 采用插件优先级高的
        PropMapper previous = mappers.get(toType);
        if (previous == null) {
            mappers.put(toType, mapper);
            if (LOG_ENABLED) {
                logger.info(LOG_PREFIX + "PropMapper Enabled from <" + fromType.getName() + "> to <" + toType.getName() + "> mapper <" + mapper.getClass().getName() + ">");
            }
        } else {
            if (LOG_ENABLED) {
                logger.info(LOG_PREFIX + "PropMapper Disabled from <" + fromType.getName() + "> to <" + toType.getName() + "> mapper <" + mapper.getClass().getName() + "> Overridden by a higher priority plugin " + previous.getClass().getName());
            }
        }
    }

    @Override
    protected Object onConvert(Cause cause, Object from, Class... toTypes) {
        if (from == null || toTypes == null || toTypes.length <= 0) {
            return null;
        }

        Class<?> fromType = from.getClass();

        //fromType match one of toTypes, if yes we will return self when convert failed
        boolean typeMatch = false;
        for (Class<?> toType : toTypes) {
            if (fromType.equals(toType)) {
                typeMatch = true;
                break;
            }
        }

        //get mappers
        Map<Class<?>, PropMapper> mappers = propMappers.get(fromType);
        if (mappers == null) {
            return typeMatch ? from : null;
        }

        //convert by spin mapper (toType = fromType)
        if (typeMatch) {
            PropMapper mapper = mappers.get(fromType);
            if (mapper == null) {
                return from;
            }
            return mapper.map(from, fromType);
        }

        //convert by cross mapper
        PropMapper mapper;
        for (Class<?> toType : toTypes) {
            mapper = mappers.get(toType);
            if (mapper != null) {
                return mapper.map(from, toType);
            }
        }

        return null;
    }

}