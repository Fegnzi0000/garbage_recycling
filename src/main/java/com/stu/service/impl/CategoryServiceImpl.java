package com.stu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.stu.entity.Category;
import com.stu.mapper.CategoryMapper;
import com.stu.service.CategoryService;
import com.stu.util.CacheConstants;
import com.stu.util.RedisCacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisCacheClient redisCacheClient;

    private static final TypeReference<List<Category>> CATEGORY_LIST_TYPE = new TypeReference<List<Category>>() {
    };

    private static final TypeReference<Category> CATEGORY_TYPE = new TypeReference<Category>() {
    };

    @Override
    public List<Category> getTopCategories() {
        return redisCacheClient.queryWithPassThrough(
                CacheConstants.CACHE_CATEGORY_TOP_KEY,
                CATEGORY_LIST_TYPE,
                () -> {
                    QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
                    queryWrapper.isNull("parent_id");
                    queryWrapper.eq("status", 1);
                    queryWrapper.orderByAsc("sort");
                    return categoryMapper.selectList(queryWrapper);
                },
                CacheConstants.CATEGORY_LIST_TTL,
                CacheConstants.CATEGORY_LIST_TTL_UNIT,
                CacheConstants.NULL_CACHE_TTL,
                CacheConstants.NULL_CACHE_TTL_UNIT
        );
    }

    @Override
    public List<Category> getSubCategoriesByParentId(Long parentId) {
        String key = CacheConstants.CACHE_CATEGORY_SUB_KEY_PREFIX + parentId;
        return redisCacheClient.queryWithPassThrough(
                key,
                CATEGORY_LIST_TYPE,
                () -> {
                    QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("parent_id", parentId);
                    queryWrapper.eq("status", 1);
                    queryWrapper.orderByAsc("sort");
                    return categoryMapper.selectList(queryWrapper);
                },
                CacheConstants.CATEGORY_LIST_TTL,
                CacheConstants.CATEGORY_LIST_TTL_UNIT,
                CacheConstants.NULL_CACHE_TTL,
                CacheConstants.NULL_CACHE_TTL_UNIT
        );
    }

    @Override
    public Category getCategoryById(Long id) {
        if (id == null) {
            return null;
        }
        String key = CacheConstants.CACHE_CATEGORY_DETAIL_KEY_PREFIX + id;
        return redisCacheClient.queryWithLogicalExpire(
                key,
                CATEGORY_TYPE,
                () -> categoryMapper.selectById(id),
                CacheConstants.CATEGORY_DETAIL_LOGICAL_EXPIRE,
                CacheConstants.CATEGORY_DETAIL_LOGICAL_EXPIRE_UNIT,
                CacheConstants.CATEGORY_DETAIL_REDIS_TTL,
                CacheConstants.CATEGORY_DETAIL_REDIS_TTL_UNIT,
                CacheConstants.NULL_CACHE_TTL,
                CacheConstants.NULL_CACHE_TTL_UNIT
        );
    }

    @Override
    public boolean isValidTopCategory(Long categoryId) {
        if (categoryId == null) {
            return false;
        }
        Category category = getCategoryById(categoryId);
        return category != null && category.getParentId() == null && category.getStatus() == 1;
    }
}