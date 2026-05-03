package com.stu.ai.tools;

import com.stu.entity.UserAddress;
import com.stu.service.UserAddressService;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 助手工具：地址相关（仅返回脱敏信息）。
 */
public class AddressAssistantTools {

    private final Long userId;
    private final UserAddressService userAddressService;

    public AddressAssistantTools(Long userId, UserAddressService userAddressService) {
        this.userId = userId;
        this.userAddressService = userAddressService;
    }

    @Tool("list_my_addresses")
    public String listMyAddresses() {
        List<UserAddress> list = userAddressService.listByUserId(userId);
        if (list == null || list.isEmpty()) {
            return "当前用户暂无地址，请先调用 /api/address/create 新增地址。";
        }

        return list.stream()
                .map(a -> String.format("id=%d, label=%s, detail=%s, isDefault=%s",
                        a.getId(),
                        safe(a.getAddressLabel()),
                        maskAddress(a.getDetailAddress()),
                        a.getIsDefault() != null && a.getIsDefault() == 1 ? "yes" : "no"))
                .collect(Collectors.joining("\n"));
    }

    private String maskAddress(String detail) {
        if (detail == null || detail.isBlank()) {
            return "";
        }
        // 简单脱敏：保留前 6 个字符
        return detail.length() <= 6 ? detail : detail.substring(0, 6) + "***";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

