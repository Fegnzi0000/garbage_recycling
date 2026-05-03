package com.stu.ai.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 创建草稿后的返回结果（给助手/前端展示用）。
 */
@Data
@AllArgsConstructor
public class OrderDraftCreateResult {

	private String draftId;

	/**
	 * 给用户展示的摘要（不包含隐私信息）。
	 */
	private String summary;
}

