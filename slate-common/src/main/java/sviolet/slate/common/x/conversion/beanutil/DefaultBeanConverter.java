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
            if (mapper.fromType() == null || mapper.toType() == null) {
                throw new RuntimeException("Invalid PropMapper " + mapper.getClass().getName() + ", fromType or toType method return null, you can ignore this plugin by ThistleSpi");
            }
            Map<Class<?>, PropMapper> mappers = propMappers.get(mapper.fromType());
            if (mappers == null) {
                mappers = new HashMap<>();
                propMappers.put(mapper.fromType(), mappers);
            }
            //遇到同类型映射器时, 采用插件优先级高的
            PropMapper previous = mappers.get(mapper.toType());
            if (previous == null) {
                mappers.put(mapper.toType(), mapper);
                if (LOG_ENABLED) {
                    logger.info(LOG_PREFIX + "PropMapper enabled fromType:" + mapper.fromType().getName() + " toType:" + mapper.toType().getName() + " mapper:" + mapper.getClass().getName());
                }
            } else {
                if (LOG_ENABLED) {
                    logger.info(LOG_PREFIX + "PropMapper disabled fromType:" + mapper.fromType().getName() + " toType:" + mapper.toType().getName() + " mapper:" + mapper.getClass().getName() + ", Overridden by a higher priority plugin");
                }
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

        //convert by spin mapper
        if (typeMatch) {
            PropMapper mapper = mappers.get(fromType);
            if (mapper == null) {
                return from;
            }
            return mapper.map(from);
        }

        //convert by cross mapper
        PropMapper mapper;
        for (Class<?> toType : toTypes) {
            mapper = mappers.get(toType);
            if (mapper != null) {
                return mapper.map(from);
            }
        }

        return null;
    }

}
