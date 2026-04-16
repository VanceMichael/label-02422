package com.blog.aspect;

import com.blog.annotation.OperationLog;
import com.blog.mapper.OperationLogMapper;
import com.blog.utils.IpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogAspect.class);

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = point.proceed();
        long endTime = System.currentTimeMillis();

        try {
            saveLog(point, operationLog, endTime - startTime);
        } catch (Exception e) {
            logger.error("保存操作日志失败", e);
        }

        return result;
    }

    private void saveLog(ProceedingJoinPoint point, OperationLog operationLog, long time) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String method = signature.getDeclaringTypeName() + "." + signature.getName();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        com.blog.entity.OperationLog operationLogEntity = new com.blog.entity.OperationLog();
        operationLogEntity.setOperation(operationLog.value());
        operationLogEntity.setMethod(method);

        try {
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                operationLogEntity.setParams(objectMapper.writeValueAsString(args));
            }
        } catch (Exception e) {
            operationLogEntity.setParams("参数序列化失败");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            operationLogEntity.setUserId((Long) authentication.getPrincipal());
        }

        if (request != null) {
            operationLogEntity.setIp(IpUtil.getIpAddress(request));
        }

        operationLogMapper.insert(operationLogEntity);
        
        logger.info("操作日志: {} - {}ms", operationLog.value(), time);
    }
}
