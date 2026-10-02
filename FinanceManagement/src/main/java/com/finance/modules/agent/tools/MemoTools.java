package com.finance.modules.agent.tools;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.finance.modules.agent.rag.RagService;
import com.finance.modules.memo.entity.Memo;
import com.finance.modules.memo.mapper.MemoMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 备忘录工具集
 */
@Component
public class MemoTools {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MemoMapper memoMapper;
    private final RagService ragService;

    public MemoTools(MemoMapper memoMapper, RagService ragService) {
        this.memoMapper = memoMapper;
        this.ragService = ragService;
    }

    @Tool(description = "查询当前用户的备忘录，支持按关键词搜索（匹配标题或内容）。" +
            "适用场景：'我记了什么关于房租的事'、'备忘录里有关于报销的吗'。不传关键词返回最近的备忘（最多50条）。")
    public String listMemos(
            @ToolParam(description = "搜索关键词，匹配备忘标题或内容；不传返回最近的备忘", required = false) String keyword,
            @ToolParam(description = "完成状态筛选：0-未完成，1-已完成；不传查全部", required = false) Integer isCompleted,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        LambdaQueryWrapper<Memo> wrapper = new LambdaQueryWrapper<Memo>()
                .eq(Memo::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            // 关键词同时匹配标题和内容，比旧的仅标题匹配更适合"那件涨租的事"这类模糊检索
            wrapper.and(w -> w.like(Memo::getTitle, keyword.trim()).or().like(Memo::getContent, keyword.trim()));
        }
        if (isCompleted != null && (isCompleted == 0 || isCompleted == 1)) {
            wrapper.eq(Memo::getIsCompleted, isCompleted);
        }
        List<Memo> memos = memoMapper.selectList(
                wrapper.orderByDesc(Memo::getCreateTime).last("LIMIT 50"));

        List<Map<String, Object>> items = new ArrayList<>();
        for (Memo m : memos) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("title", m.getTitle());
            item.put("content", m.getContent());
            item.put("isCompleted", m.getIsCompleted() != null && m.getIsCompleted() == 1 ? "已完成" : "未完成");
            item.put("remindTime", m.getRemindTime() != null ? m.getRemindTime().format(DATETIME) : null);
            item.put("createTime", m.getCreateTime() != null ? m.getCreateTime().format(DATETIME) : null);
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", items.size());
        result.put("records", items);
        return JSONUtil.toJsonStr(result);
    }

    @Tool(description = "为当前用户创建一条备忘录。" +
            "适用场景：'把房东说涨租5%这件事记一下'、'提醒我周三交房租'。title 为标题，content 为具体内容。")
    public String createMemo(
            @ToolParam(description = "备忘标题，1-100字，如'房租涨价'") String title,
            @ToolParam(description = "备忘具体内容，如'房东说明年房租上涨5%'；可null", required = false) String content,
            @ToolParam(description = "提醒时间，格式yyyy-MM-dd HH:mm:ss；可null", required = false) String remindTime,
            ToolContext toolContext) {

        Long userId = AgentToolSupport.currentUserId(toolContext);
        if (!StringUtils.hasText(title) || title.length() > 100) {
            return "失败：备忘标题不能为空且不超过100字";
        }

        Memo memo = new Memo();
        memo.setUserId(userId);
        memo.setTitle(title.trim());
        memo.setContent(content);
        if (StringUtils.hasText(remindTime)) {
            try {
                memo.setRemindTime(LocalDateTime.parse(remindTime.trim(), DATETIME));
            } catch (Exception e) {
                return "失败：提醒时间格式错误，应为yyyy-MM-dd HH:mm:ss，例如 2026-10-01 09:00:00";
            }
        }
        memo.setIsCompleted(0);
        memoMapper.insert(memo);

        ragService.indexMemo(memo); // 增量更新向量索引

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("id", memo.getId());
        result.put("message", "已创建备忘'" + memo.getTitle() + "'");
        return JSONUtil.toJsonStr(result);
    }
}
