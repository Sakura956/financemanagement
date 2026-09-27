<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { memoApi } from '@/api/modules/memo'
import type { Memo, MemoForm, PageResult } from '@/types'
import { formatDate,formatDateTimeDeleteT } from '@/utils'
import { Plus, Search } from '@element-plus/icons-vue'

const listData = ref<PageResult<Memo> | null>(null)//存储备忘录列表 + 分页数据
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建备忘录')
const editingId = ref<number | null>(null)

const statusFilter = ref<0 | 1 | undefined>(undefined)//状态筛选
const keyword = ref('')
const page = ref(1)

const formRef = ref<FormInstance>()
const form = reactive<MemoForm>({ title: '', content: '', remindTime: '' })
const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
}

//加载备忘录数据
async function fetchMemos() {
  loading.value = true
  try {
    listData.value = await memoApi.getMemos({
      isCompleted: statusFilter.value,
      keyword: keyword.value || undefined,
      page: page.value,
      size: 12,
    })
  } finally {
    loading.value = false
  }
}

//打开新建 / 编辑弹窗
function openCreateDialog() {
  dialogTitle.value = '新建备忘录'
  editingId.value = null
  form.title = ''
  form.content = ''
  form.remindTime = ''
  dialogVisible.value = true
}
//打开修改弹窗
function openEditDialog(row: Memo) {
  dialogTitle.value = '修改备忘录'
  //回显
  editingId.value = row.id
  form.title = row.title
  form.content = row.content || ''
  form.remindTime = row.remindTime || ''
  dialogVisible.value = true
}
//提交表单
async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await memoApi.updateMemo(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await memoApi.createMemo({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchMemos()
  } catch {  }
}
//切换完成状态
async function handleToggle(row: Memo) {
  try {
    const result = await memoApi.toggleMemo(row.id)
    row.isCompleted = result.isCompleted
    row.updateTime = result.updateTime
    ElMessage.success(result.isCompleted ? '已完成' : '已取消完成')
  } catch {  }
}
//删除备忘录
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该备忘录吗？', '确认删除', { type: 'warning' })
  try {
    await memoApi.deleteMemo(id)
    ElMessage.success('删除成功')
    fetchMemos()
  } catch {  }
}

onMounted(fetchMemos)
</script>

<template>
  <div class="memo-page">
    <div class="page-header">
      <h3 class="page-title">备忘录</h3>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建备忘录</el-button>
    </div>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-row :gutter="16" align="middle">
        <el-col :span="6">
          <el-radio-group v-model="statusFilter" @change="page = 1; fetchMemos()">
            <el-radio-button :value="undefined">全部</el-radio-button>
            <el-radio-button :value="0">未完成</el-radio-button>
            <el-radio-button :value="1">已完成</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :span="8">
          <el-input v-model="keyword" placeholder="搜索标题" :prefix-icon="Search" clearable @clear="page = 1; fetchMemos()"
            @keyup.enter="page = 1; fetchMemos()" />
        </el-col>
        <el-col :span="2">
          <el-button type="primary" @click="page = 1; fetchMemos()">搜索</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Memo Grid -->
    <div v-loading="loading">
      <el-empty v-if="listData && listData.records.length === 0" description="暂无备忘录" />
      <el-row v-else :gutter="16">
        <el-col v-for="memo in listData?.records" :key="memo.id" :span="8" style="margin-bottom: 16px">
          <el-card shadow="never" class="memo-card" :class="{ completed: memo.isCompleted === 1 }">
            <div class="memo-title-row">
              <h4 class="memo-title" :class="{ done: memo.isCompleted }">{{ memo.title }}</h4>
              <el-tag :type="memo.isCompleted ? 'success' : 'warning'" size="small" effect="light">
                {{ memo.isCompleted ? '已完成' : '未完成' }}
              </el-tag>
            </div>
            <p class="memo-content" v-if="memo.content">{{ memo.content }}</p>
            <div class="memo-meta">
              <span v-if="memo.remindTime" class="memo-remind">
                <el-icon>
                  <Clock />
                </el-icon> {{ formatDateTimeDeleteT(formatDate(memo.remindTime)) }}
              </span>
              <span class="memo-date">{{ formatDateTimeDeleteT(formatDate(memo.createTime)) }}</span>
            </div>
            <div class="memo-actions">
              <el-button type="primary" link size="small" @click="openEditDialog(memo)">编辑</el-button>
              <el-button :type="memo.isCompleted ? 'warning' : 'success'" link size="small" @click="handleToggle(memo)">
                {{ memo.isCompleted ? '取消完成' : '完成' }}
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(memo.id)">删除</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div class="pagination-wrap" v-if="listData && listData.total > 0">
        <el-pagination background layout="total, prev, pager, next" :total="listData.total" :page-size="listData.size"
          :current-page="listData.page" @current-change="(p: number) => { page = p; fetchMemos() }" />
      </div>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="提醒时间" prop="remindTime">
          <el-date-picker v-model="form.remindTime" type="datetime" placeholder="选填" value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.memo-card {
  transition: box-shadow 0.2s;
}

.memo-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.memo-card.completed {
  opacity: 0.72;
}

.memo-title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.memo-title {
  font-size: 15px;
  color: #1e293b;
  margin: 0;
  flex: 1;
  margin-right: 8px;
}

.memo-title.done {
  text-decoration: line-through;
  color: #94a3b8;
}

.memo-content {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px;
  line-height: 1.6;
}

.memo-meta {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
  display: flex;
  gap: 16px;
}

.memo-remind {
  display: flex;
  align-items: center;
  gap: 4px;
}

.memo-actions {
  border-top: 1px solid #f1f5f9;
  padding-top: 8px;
  display: flex;
  gap: 4px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
