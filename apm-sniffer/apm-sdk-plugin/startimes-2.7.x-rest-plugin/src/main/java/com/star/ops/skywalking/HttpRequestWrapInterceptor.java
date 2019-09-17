/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.star.ops.skywalking;

import org.apache.dubbo.rpc.Result;
import org.apache.logging.log4j.ThreadContext;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.util.StringUtil;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.container.ContainerRequestContext;
import java.lang.reflect.Method;

public class HttpRequestWrapInterceptor implements InstanceMethodsAroundInterceptor {
    private static final String SW_TRACE_ID = "SW-TraceId";
    private static final String DEPLOY_GROUP = "SG-N";

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                             Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object filterArg = allArguments[0];
        //发起请求时将当前调用group中的值写入
        if (filterArg instanceof ClientRequestContext) {
            if (ThreadContext.containsKey(DEPLOY_GROUP)) {
                ClientRequestContext requestContext = (ClientRequestContext) filterArg;
                requestContext.getHeaders().putSingle(DEPLOY_GROUP, ThreadContext.get(DEPLOY_GROUP));
            }
        } else if (filterArg instanceof ContainerRequestContext) { //接受请求的时候存下来
            if (!ThreadContext.containsKey(DEPLOY_GROUP)) {
                ContainerRequestContext requestContext = (ContainerRequestContext) filterArg;
                String deployGroup = requestContext.getHeaderString(DEPLOY_GROUP);
                if (StringUtil.isEmpty(deployGroup)) {
                    ThreadContext.put(DEPLOY_GROUP, deployGroup);
                }
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }

}
