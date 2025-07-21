package com.example.graph.mcp.context;

import org.springframework.stereotype.Component;

/**
 * 租户上下文管理器
 * 用于在请求生命周期中存储和获取租户ID
 */
@Component
public class TenantContext {
    
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();
    
    /**
     * 设置当前请求的租户ID
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    /**
     * 获取当前请求的租户ID
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }
    
    /**
     * 清除当前请求的租户ID
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}