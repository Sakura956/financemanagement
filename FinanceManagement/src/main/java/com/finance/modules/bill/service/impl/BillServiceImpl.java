package com.finance.modules.bill.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.finance.common.exception.BusinessException;
import com.finance.common.result.PageResult;
import com.finance.modules.bill.dto.BillCreateRequest;
import com.finance.modules.bill.dto.BillUpdateRequest;
import com.finance.modules.bill.dto.BillVO;
import com.finance.modules.bill.entity.BillCategory;
import com.finance.modules.bill.entity.BillRecord;
import com.finance.modules.bill.mapper.BillCategoryMapper;
import com.finance.modules.bill.mapper.BillRecordMapper;
import com.finance.modules.bill.service.BillService;
import com.finance.util.CacheClient;
import com.finance.util.SecurityUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BillServiceImpl implements BillService {

    private final BillRecordMapper billRecordMapper;
    private final BillCategoryMapper billCategoryMapper;
    // 注入缓存工具
    private final CacheClient cacheClient;
    // 注入 StringRedisTemplate，用于缓存 Key 的模式匹配清理（CacheClient 不提供 keys/批量删除能力）
    private final StringRedisTemplate stringRedisTemplate;
    public BillServiceImpl(BillRecordMapper billRecordMapper, BillCategoryMapper billCategoryMapper,
                           CacheClient cacheClient, StringRedisTemplate stringRedisTemplate) {
        this.billRecordMapper = billRecordMapper;
        this.billCategoryMapper = billCategoryMapper;
        this.cacheClient = cacheClient;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 缓存 KEY 前缀
    private static final String CACHE_BILL_LIST = "cache:bill:list:";
    // 缓存过期时间 5 分钟
    private static final long CACHE_TTL = 5L;

    /**
     * 清理当前用户的所有账单列表缓存 + 统计数据缓存
     * 创建/修改/删除账单时调用，因为账单数据变更会导致统计结果也失效
     */
    private void clearUserBillCache(Long userId) {
        // 清理账单列表缓存
        String userCachePrefix = CACHE_BILL_LIST + userId + ":*";
        //从 Redis 查出所有匹配的 key
        Set<String> keys = stringRedisTemplate.keys(userCachePrefix);
        if (CollUtil.isNotEmpty(keys)) {
            stringRedisTemplate.delete(keys);//删除所有匹配的缓存
        }
        // 同时清理统计缓存（账单数据变了 → 月度概览、饼图、趋势等统计全部失效）
        // 匹配 cache:statistics:overview:{userId}:* 和 cache:statistics:records:{userId}:*
        String statsPattern = "cache:statistics:*:" + userId + ":*";
        Set<String> statsKeys = stringRedisTemplate.keys(statsPattern);
        if (CollUtil.isNotEmpty(statsKeys)) {
            stringRedisTemplate.delete(statsKeys);
        }
    }

    @Override
    public Object createBill(BillCreateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        // 校验分类
        BillCategory category = billCategoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() == 1) {
            throw new BusinessException(400, "分类不存在或已禁用");
        }
        if (!category.getType().equals(request.getType())) {
            throw new BusinessException(400, "选择的分类与账单类型不匹配");
        }
        // 校验recordTime不能是未来时间
        LocalDateTime recordTime = LocalDateTime.parse(request.getRecordTime(),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (recordTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(400, "记录时间不能是未来时间");
        }

        BillRecord record = new BillRecord();
        record.setUserId(userId);
        record.setType(request.getType());
        record.setAmount(request.getAmount());
        record.setCategoryId(request.getCategoryId());
        record.setDescription(request.getDescription());
        record.setRecordTime(recordTime);
        billRecordMapper.insert(record);

        clearUserBillCache(userId);//清理缓存

        Map<String, Object> result = new HashMap<>();
        result.put("id", record.getId());
        result.put("type", record.getType());
        result.put("amount", record.getAmount());
        result.put("categoryId", record.getCategoryId());
        result.put("categoryName", category.getName());
        result.put("categoryIcon", category.getIcon());
        result.put("description", record.getDescription());
        result.put("recordTime", request.getRecordTime());
        result.put("createTime", record.getCreateTime() != null ? record.getCreateTime().toString() : null);
        return result;
    }

    @Override
    public Object listBills(int page, int size, Integer type, Long categoryId,
                                   String startDate, String endDate, String keyword,
                                   Double minAmount, Double maxAmount, String sortBy, String order) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 拼接唯一缓存Key
        String cacheKey = String.format("%s%d:%d:%d:%s:%s:%s:%s:%s:%s:%s:%s:%s",
                CACHE_BILL_LIST,
                userId,
                page,
                size,
                type == null ? "none" : type,
                categoryId == null ? "none" : categoryId,
                startDate == null ? "none" : startDate,
                endDate == null ? "none" : endDate,
                keyword == null ? "none" : keyword,
                minAmount == null ? "none" : minAmount,
                maxAmount == null ? "none" : maxAmount,
                sortBy == null ? "none" : sortBy,
                order == null ? "none" : order
        );

        // 先从缓存取
        Map<String, Object> cacheResult = cacheClient.queryWithPassThrough(
                "", // 因为我们自己拼了完整key，这里传空
                cacheKey,
                Map.class, // 返回类型
                id -> loadBillDataFromDb(
                        page, size, type, categoryId,
                        startDate, endDate, keyword,
                        minAmount, maxAmount, sortBy, order, userId
                ), // 查库逻辑（抽成方法）
                CACHE_TTL,
                TimeUnit.MINUTES
        );

        return cacheResult;

    }

    /**
     * 真正从数据库查询账单数据（抽出来给缓存工具调用）
     */
    private Map<String, Object> loadBillDataFromDb(int page, int size, Integer type, Long categoryId,
                                                   String startDate, String endDate, String keyword,
                                                   Double minAmount, Double maxAmount, String sortBy, String order,
                                                   Long userId) {
        LambdaQueryWrapper<BillRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BillRecord::getUserId, userId);
        if (type != null) {
            wrapper.eq(BillRecord::getType, type);
        }
        if (categoryId != null) {
            wrapper.eq(BillRecord::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(BillRecord::getRecordTime, startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(BillRecord::getRecordTime, endDate + " 23:59:59");
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(BillRecord::getDescription, keyword);
        }
        if (minAmount != null) {
            wrapper.ge(BillRecord::getAmount, minAmount);
        }
        if (maxAmount != null) {
            wrapper.le(BillRecord::getAmount, maxAmount);
        }
        // 排序
        if ("amount".equals(sortBy)) {
            if ("asc".equalsIgnoreCase(order)) {
                wrapper.orderByAsc(BillRecord::getAmount);
            } else {
                wrapper.orderByDesc(BillRecord::getAmount);
            }
        } else {
            if ("asc".equalsIgnoreCase(order)) {
                wrapper.orderByAsc(BillRecord::getRecordTime);
            } else {
                wrapper.orderByDesc(BillRecord::getRecordTime);
            }
        }

        Page<BillRecord> result = billRecordMapper.selectPage(new Page<>(page, size), wrapper);

        // 构建VO列表
        List<BillVO> records = new ArrayList<>();
        for (BillRecord record : result.getRecords()) {
            BillCategory category = billCategoryMapper.selectById(record.getCategoryId());
            BillVO vo = new BillVO();
            vo.setId(record.getId());
            vo.setType(record.getType());
            vo.setAmount(record.getAmount());
            vo.setCategoryId(record.getCategoryId());
            vo.setCategoryName(category != null ? category.getName() : null);
            vo.setCategoryIcon(category != null ? category.getIcon() : null);
            vo.setDescription(record.getDescription());
            vo.setRecordTime(record.getRecordTime() != null ? record.getRecordTime().toString() : null);
            vo.setCreateTime(record.getCreateTime() != null ? record.getCreateTime().toString() : null);
            vo.setUpdateTime(record.getUpdateTime() != null ? record.getUpdateTime().toString() : null);
            records.add(vo);
        }

        //  使用 SQL SUM 聚合，让数据库只返回一个数字，避免 selectList 全表拉取
        // 统计总收入：SELECT SUM(amount) WHERE type=1
        QueryWrapper<BillRecord> incomeSumWrapper = new QueryWrapper<BillRecord>()
                .select("IFNULL(SUM(amount), 0) AS total")     // SELECT SUM(amount)
                .eq("user_id", userId)                         // WHERE user_id = ?
                .eq("type", 1);                                // AND type = 1（收入）
        if (StringUtils.hasText(startDate)) {
            incomeSumWrapper.ge("record_time", startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            incomeSumWrapper.le("record_time", endDate + " 23:59:59");
        }
        if (StringUtils.hasText(keyword)) {
            incomeSumWrapper.like("description", keyword);
        }
        Double totalIncome = ((Number) billRecordMapper.selectMaps(incomeSumWrapper)  // 只返回1行1列
                .get(0).get("total")).doubleValue();

        // 统计总支出：SELECT SUM(amount) WHERE type=0
        QueryWrapper<BillRecord> expenseSumWrapper = new QueryWrapper<BillRecord>()
                .select("IFNULL(SUM(amount), 0) AS total")     // SELECT SUM(amount)
                .eq("user_id", userId)                         // WHERE user_id = ?
                .eq("type", 0);                                // AND type = 0（支出）
        if (StringUtils.hasText(startDate)) {
            expenseSumWrapper.ge("record_time", startDate + " 00:00:00");
        }
        if (StringUtils.hasText(endDate)) {
            expenseSumWrapper.le("record_time", endDate + " 23:59:59");
        }
        if (StringUtils.hasText(keyword)) {
            expenseSumWrapper.like("description", keyword);
        }
        Double totalExpense = ((Number) billRecordMapper.selectMaps(expenseSumWrapper)  // 只返回1行1列
                .get(0).get("total")).doubleValue();

        PageResult<BillVO> pageResult = new PageResult<>(result.getTotal(), page, size, records);

        Map<String, Object> response = new HashMap<>();
        response.put("total", pageResult.getTotal());
        response.put("page", pageResult.getPage());
        response.put("size", pageResult.getSize());
        response.put("pages", pageResult.getPages());
        response.put("records", pageResult.getRecords());
        Map<String, Double> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        response.put("summary", summary);

        return response;
    }

    @Override
    public Object getBillDetail(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        BillRecord record = billRecordMapper.selectOne(
                new LambdaQueryWrapper<BillRecord>()
                        .eq(BillRecord::getId, id)
                        .eq(BillRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(404, "账单不存在");
        }
        BillCategory category = billCategoryMapper.selectById(record.getCategoryId());
        BillVO vo = new BillVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        vo.setAmount(record.getAmount());
        vo.setCategoryId(record.getCategoryId());
        vo.setCategoryName(category != null ? category.getName() : null);
        vo.setCategoryIcon(category != null ? category.getIcon() : null);
        vo.setDescription(record.getDescription());
        vo.setRecordTime(record.getRecordTime() != null ? record.getRecordTime().toString() : null);
        vo.setCreateTime(record.getCreateTime() != null ? record.getCreateTime().toString() : null);
        vo.setUpdateTime(record.getUpdateTime() != null ? record.getUpdateTime().toString() : null);
        return vo;
    }

    @Override
    public Object updateBill(Long id, BillUpdateRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        BillRecord record = billRecordMapper.selectOne(
                new LambdaQueryWrapper<BillRecord>()
                        .eq(BillRecord::getId, id)
                        .eq(BillRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(404, "账单不存在");
        }
        // 校验分类
        if (request.getCategoryId() != null) {
            BillCategory category = billCategoryMapper.selectById(request.getCategoryId());
            if (category == null || category.getStatus() == 1) {
                throw new BusinessException(400, "分类不存在或已禁用");
            }
            if (!category.getType().equals(record.getType())) {
                throw new BusinessException(400, "选择的分类与账单类型不匹配");
            }
            record.setCategoryId(request.getCategoryId());
        }
        if (request.getAmount() != null) {
            record.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            record.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getRecordTime())) {
            record.setRecordTime(LocalDateTime.parse(request.getRecordTime(),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        billRecordMapper.updateById(record);

        clearUserBillCache(userId);//清理缓存

        BillCategory category = billCategoryMapper.selectById(record.getCategoryId());
        BillVO vo = new BillVO();
        vo.setId(record.getId());
        vo.setType(record.getType());
        vo.setAmount(record.getAmount());
        vo.setCategoryId(record.getCategoryId());
        vo.setCategoryName(category != null ? category.getName() : null);
        vo.setCategoryIcon(category != null ? category.getIcon() : null);
        vo.setDescription(record.getDescription());
        vo.setRecordTime(record.getRecordTime() != null ? record.getRecordTime().toString() : null);
        vo.setUpdateTime(record.getUpdateTime() != null ? record.getUpdateTime().toString() : null);
        return vo;
    }

    @Override
    public void deleteBill(Long id) {
        Long userId = SecurityUtil.getCurrentUserId();
        BillRecord record = billRecordMapper.selectOne(
                new LambdaQueryWrapper<BillRecord>()
                        .eq(BillRecord::getId, id)
                        .eq(BillRecord::getUserId, userId));
        if (record == null) {
            throw new BusinessException(404, "账单不存在");
        }
        billRecordMapper.deleteById(id);

        clearUserBillCache(userId);//清理缓存
    }

    @Override
    public int batchDeleteBills(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Long userId = SecurityUtil.getCurrentUserId();
        int count = 0;
        for (Long id : ids) {
            BillRecord record = billRecordMapper.selectOne(
                    new LambdaQueryWrapper<BillRecord>()
                            .eq(BillRecord::getId, id)
                            .eq(BillRecord::getUserId, userId));
            if (record != null) {
                billRecordMapper.deleteById(id);
                count++;
            }
        }

        if (count > 0) {
            clearUserBillCache(userId);
        }

        return count;
    }
}
