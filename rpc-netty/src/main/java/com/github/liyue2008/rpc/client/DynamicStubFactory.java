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
package com.github.liyue2008.rpc.client;

import com.github.liyue2008.rpc.transport.Transport;
import com.itranswarp.compiler.JavaStringCompiler;


import java.util.Map;

/**
 * @author LiYue
 * Date: 2019/9/27
 */
public class DynamicStubFactory implements StubFactory{


    /**
     *     %s	字符串类型
     */
    private final static String STUB_SOURCE_TEMPLATE =
            "package com.github.liyue2008.rpc.client.stubs;\n" +
            "import com.github.liyue2008.rpc.serialize.SerializeSupport;\n" +
            "\n" +
            "public class %s extends AbstractStub implements %s {\n" +
            "    @Override\n" +
            "    public String %s(String arg) {\n" +
            "        return SerializeSupport.parse(\n" +
            "                invokeRemote(\n" +
            "                        new RpcRequest(\n" +
            "                                \"%s\",\n" +
            "                                \"%s\",\n" +
            "                                SerializeSupport.serialize(arg)\n" +
            "                        )\n" +
            "                )\n" +
            "        );\n" +
            "    }\n" +
            "}";

    @Override
    @SuppressWarnings("unchecked")
    public <T> T createStub(Transport transport, Class<T> serviceClass) {
        try {
            // 填充模板
            //HelloServiceStub
            String stubSimpleName = serviceClass.getSimpleName() + "Stub";
            //com.github.liyue2008.rpc.hello.HelloService
            String classFullName = serviceClass.getName();
            //com.github.liyue2008.rpc.client.stubs.HelloServiceStub
            String stubFullName = "com.github.liyue2008.rpc.client.stubs." + stubSimpleName;
            // hello
            String methodName = serviceClass.getMethods()[0].getName();

            //填充源代码模板 把STUB_SOURCE_TEMPLATE 中的 %s 用后面的参数替换


            /*  填充后的代码示例

            package com.github.liyue2008.rpc.client.stubs;
            import com.github.liyue2008.rpc.serialize.SerializeSupport;

            public class HelloServiceStub extends AbstractStub implements com.github.liyue2008.rpc.hello.HelloService {
                @Override
                public String hello(String arg) {
                    return SerializeSupport.parse(
                            invokeRemote(
                                    new RpcRequest(
                                            "com.github.liyue2008.rpc.hello.HelloService",
                                            "hello",
                                            SerializeSupport.serialize(arg)
                                    )
                            )
                    );
                }
            }
            */
            String source = String.format(STUB_SOURCE_TEMPLATE, stubSimpleName, classFullName, methodName, classFullName, methodName);
            // 编译源代码
            JavaStringCompiler compiler = new JavaStringCompiler();
            Map<String, byte[]> results = compiler.compile(stubSimpleName + ".java", source);
            // 加载编译好的类
            Class<?> clazz = compiler.loadClass(stubFullName, results);

            // 反射获取实例
            ServiceStub stubInstance = (ServiceStub) clazz.newInstance();
            // 把Transport赋值给桩
            stubInstance.setTransport(transport);
            // 返回这个桩
            return (T) stubInstance;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
