package com.itman.datakit.admin.common.dataroute;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RouteAspect {
    @Pointcut("execution(* com.itman.datakit.admin.dao.*.*(..))")
    public void pointCutDao() {
    }

    @Pointcut("execution(* com.itman.datakit.admin.common.tablemeta.TableMetaGenerator.*(..))")
    public void pointCutTableMeta() {
    }


    @Pointcut("pointCutDao() || pointCutTableMeta()")
    private void pointCut() {
    }

    @Before(value = "pointCut()")
    public void doBefore(JoinPoint joinPoint) {
        try {
            Object[] parameterValue = joinPoint.getArgs();

            RouteContext.setRouteKey((parameterValue.length > 0 &&
                    parameterValue[0].toString().length() >= 3 &&
                    parameterValue[0].toString().substring(0, 2).equals("db")) ? parameterValue[0].toString() : "db0");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After(value = "pointCut()")
    public void doAfter(JoinPoint joinPoint) {
        RouteContext.removeRouteKey();
    }

    @Around(value = "pointCut()")
    public Object doAround(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        Object o = pjp.proceed(args);
        return o;
    }
}
