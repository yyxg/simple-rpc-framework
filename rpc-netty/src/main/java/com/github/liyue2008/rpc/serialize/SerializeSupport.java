/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.liyue2008.rpc.serialize;

import com.github.liyue2008.rpc.spi.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LiYue
 * Date: 2019/9/20
 */
@SuppressWarnings("unchecked")
public class SerializeSupport {
    private static final Logger logger = LoggerFactory.getLogger(SerializeSupport.class);
    /**
     *Class<?> 序列化对象类型,即 需要对哪一种对象进行序列化
     *
     * Serializer<?> 序列化实现类，每一种对象对应着一个序列化的实现类
     *
     *
     * String.class ,StringSerializer
     */
    private static Map<Class<?>, Serializer<?>> serializerMap = new HashMap<>();

    /**
     *Byte序列化实现类型
     *
     * Class<?> 序列化对象类型
     *
     * 0 ,String.class
     */
    private static Map<Byte, Class<?>> typeMap = new HashMap<>();

    /**
     * 在SerializeSupport 初始化的时候 会执行static 加载
     *
     */
    static {
        for (Serializer serializer : ServiceSupport.loadAll(Serializer.class)) {
            registerType(serializer.type(), serializer.getSerializeClass(), serializer);
            logger.info("Found serializer, class: {}, type: {}.",
                    serializer.getSerializeClass().getCanonicalName(),
                    serializer.type());
        }
    }

    /**
     * 在序列化的时候把类型 放在了数组的第一位
     * @param buffer
     * @return
     */
    private static byte parseEntryType(byte[] buffer) {
        return buffer[0];
    }
    private static <E> void registerType(byte type, Class<E> eClass, Serializer<E> serializer) {
        serializerMap.put(eClass, serializer);
        typeMap.put(type, eClass);
    }
    @SuppressWarnings("unchecked")
    private static  <E> E parse(byte [] buffer, int offset, int length, Class<E> eClass) {
        Object entry =  serializerMap.get(eClass).parse(buffer, offset, length);
        if (eClass.isAssignableFrom(entry.getClass())) {
            return (E) entry;
        } else {
            throw new SerializeException("Type mismatch!");
        }
    }
    public static  <E> E parse(byte [] buffer) {
        return parse(buffer, 0, buffer.length);
    }

    private static  <E> E parse(byte[] buffer, int offset, int length) {
        // 通过序列化实现类的类型 找到 序列化对象类型  再去找对应的序列化实现类 去反序列化
        byte type = parseEntryType(buffer);
        @SuppressWarnings("unchecked")
        Class<E> eClass = (Class<E> )typeMap.get(type);
        if(null == eClass) {
            throw new SerializeException(String.format("Unknown entry type: %d!", type));
        } else {
            return parse(buffer, offset + 1, length - 1,eClass);
        }

    }

    /**
     * 序列化
     * @param entry
     * @param <E>
     * @return
     */
    public static <E> byte [] serialize(E  entry) {
        @SuppressWarnings("unchecked")
        Serializer<E> serializer = (Serializer<E>) serializerMap.get(entry.getClass());
        if(serializer == null) {
            throw new SerializeException(String.format("Unknown entry class type: %s", entry.getClass().toString()));
        }
        byte [] bytes = new byte [serializer.size(entry) + 1];
        bytes[0] = serializer.type();
        serializer.serialize(entry, bytes, 1, bytes.length - 1);
        return bytes;
    }
}
