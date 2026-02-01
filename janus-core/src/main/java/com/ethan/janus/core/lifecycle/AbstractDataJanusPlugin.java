package com.ethan.janus.core.lifecycle;

import com.ethan.janus.core.exception.JanusException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class AbstractDataJanusPlugin<T> implements JanusPlugin {

    // 当前插件实现类的 class
    @SuppressWarnings("rawtypes")
    private final Class<? extends AbstractDataJanusPlugin> thisClass;
    // 插件数据实体类的 class
    private final Class<T> janusPluginDataClass;

    public AbstractDataJanusPlugin() {
        thisClass = this.getClass();
        // 获取当前实现类的 Class（在运行时，this 指向的是实现类的实例）
        Type type = thisClass.getGenericSuperclass();

        // 判断是否是参数化类型 (即带有泛型参数的类型)
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;

            // 获取泛型数组中的第一个参数 (即 T 的类型)
            //noinspection unchecked
            this.janusPluginDataClass = (Class<T>) pType.getActualTypeArguments()[0];
        } else {
            this.janusPluginDataClass = null;
        }
    }

    /**
     * 获取插件数据对象
     * <p>如果没找到，会自动通过反射new数据对象
     *
     * @return 插件数据对象
     */
    protected final T getPluginData(JanusContext context) {
        Object pluginDataObj = context.getPluginData(thisClass);
        if (pluginDataObj != null) {
            //noinspection unchecked
            return (T) pluginDataObj;
        }
        if (this.janusPluginDataClass != null) {
            try {
                T pluginData = janusPluginDataClass.getDeclaredConstructor().newInstance();
                context.putPluginData(thisClass, pluginData);
                return pluginData;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new JanusException("请设置插件数据实体类的泛型");
        }
    }
}
