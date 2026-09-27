<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { billApi } from '@/api/modules/bill'
import type { Bill, BillQuery, BillForm, Category, BillListData } from '@/types'
import { formatCurrency, formatDate, today, now,formatDateTimeDeleteT } from '@/utils'
import { Search, Plus, Delete } from '@element-plus/icons-vue'

const listData = ref<BillListData | null>(null)// 表格数据
const loading = ref(false) // 加载状态
const dialogVisible = ref(false)// 弹窗显示隐藏
const dialogTitle = ref('记一笔')
const editingId = ref<number | null>(null)
const categories = ref<Category[]>([])// 分类列表
const selectedIds = ref<number[]>([])// 批量选择的ID

//筛选查询条件（传给后端）
const query = reactive<BillQuery>({
  page: 1,
  size: 10,
  type: undefined,// 0支出 1收入
  startDate: '',
  endDate: '',
  keyword: '',
  sortBy: 'recordTime',
  order: 'desc',
})

const formRef = ref<FormInstance>()
//表单（新增 / 修改账单用）
const form = reactive<BillForm>({
  type: 0,
  amount: 0,
  categoryId: 0,
  description: '',
  recordTime: now(),
})
//表单校验规则
const formRules: FormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  recordTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
}

//根据收支类型筛选分类
//选支出只显示支出分类,选收入只显示收入分类
const filteredCategories = computed(() =>
  categories.value.filter((c) => c.type === form.type)
)

//获取账单列表
async function fetchBills() {
  loading.value = true
  try {
    listData.value = await billApi.getBills(query)
    // console.log('账单查询返回数据',listData.value)
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}
//获取分类列表
async function fetchCategories() {
  try {
    categories.value = await billApi.getCategories()
    // console.log('用户端获取的分类列表：', categories.value)
  } catch { }
}
//重置查询账单
function resetQuery() {
  query.page = 1
  query.type = undefined
  query.startDate = ''
  query.endDate = ''
  query.keyword = ''
  fetchBills()
}
//修改页数
function handlePageChange(page: number) {
  query.page = page
  fetchBills()
}
//修改每页多少条数据
function handleSizeChange(size: number) {
  query.size = size
  query.page = 1
  fetchBills()
}
//打开新增弹窗
function openCreateDialog() {
  dialogTitle.value = '记一笔'
  editingId.value = null
  form.type = 0
  form.amount = 0
  form.categoryId = 0
  form.description = ''
  form.recordTime = now()
  dialogVisible.value = true
}
//打开修改弹窗
function openEditDialog(row: Bill) {
  dialogTitle.value = '修改账单'
  editingId.value = row.id
  form.type = row.type
  form.amount = row.amount
  form.categoryId = row.categoryId
  form.description = row.description || ''
  form.recordTime = row.recordTime
  dialogVisible.value = true
}
//提交表单
async function handleSubmit() {
  if (!formRef.value) {
    ElMessage.warning('表单未就绪，请稍后再试')
    return
  }
  try {
    await formRef.value.validate()// 先校验
  } catch {
    return
  }

  //时间格式里去掉T
  form.recordTime = formatDateTimeDeleteT(form.recordTime)

  try {
    if (editingId.value) {
      const { type, ...rest } = form
      await billApi.updateBill(editingId.value, rest)// 修改
      ElMessage.success('修改成功')
    } else {
      await billApi.createBill({ ...form })// 新增
      ElMessage.success('记账成功')
    }
    dialogVisible.value = false
    fetchBills()// 刷新列表
  } catch { }
}
// 删除
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除这条账单吗？', '确认删除', { type: 'warning' })
  try {
    await billApi.deleteBill(id)
    ElMessage.success('删除成功')
    fetchBills()
  } catch { }
}
//批量删除
async function handleBatchDelete() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请选择要删除的账单')
    return
  }
  await ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条账单吗？`, '批量删除', { type: 'warning' })
  try {
    const result = await billApi.batchDeleteBills(selectedIds.value)
    ElMessage.success(`已删除 ${result.deletedCount} 条`)
    fetchBills()
  } catch { }
}
//分页切换
function handleSelectionChange(rows: Bill[]) {
  selectedIds.value = rows.map((r) => r.id)
}

onMounted(() => {
  fetchBills()// 获取账单
  fetchCategories()// 获取分类
})
</script>

<template>
  <div class="bill-page">
    <div class="page-header">
      <h3 class="page-title">收支账单</h3>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">记一笔</el-button>
        <el-button type="danger" :icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- Summary -->
    <el-row :gutter="20" style="margin-bottom: 16px" v-if="listData?.summary">
      <el-col :span="12">
        <el-card shadow="never" class="summary-card">
          <span class="summary-label">收入合计：</span>
          <span class="summary-value income">{{ formatCurrency(listData?.summary.totalIncome || 0) }}</span>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="summary-card">
          <span class="summary-label">支出合计：</span>
          <span class="summary-value expense">{{ formatCurrency(listData?.summary.totalExpense || 0) }}</span>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query">

        <el-form-item label="类型">
          <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
            <el-option label="支出" :value="0" />
            <el-option label="收入" :value="1" />
          </el-select>
        </el-form-item>

        <el-form-item label="日期">
          <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD"
            style="width: 150px"  />
          <span style="margin: 0 6px">至</span>
          <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width: 150px"  />
        </el-form-item>

        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索备注" clearable style="width: 160px" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="fetchBills()">搜索</el-button>
          <el-button
            @click="query.startDate = ''; query.endDate = ''; query.keyword = ''; query.type = undefined; resetQuery()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="listData?.records || []" v-loading="loading" stripe @selection-change="handleSelectionChange"
        style="width: 100%">

        <el-table-column type="selection" width="50" />

        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? 'danger' : 'success'" effect="light">
              {{ row.type === 0 ? '支出' : '收入' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="amount" label="金额" width="140" sortable>
          <template #default="{ row }">
            <span :style="{ color: row.type === 0 ? '#dc2626' : '#16a34a', fontWeight: 600 }">
              {{ formatCurrency(row.amount) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="categoryName" label="分类" width="120" />

        <el-table-column prop="description" label="备注" min-width="200" show-overflow-tooltip />

        <el-table-column prop="recordTime" label="记录时间" width="170">
          <template #default="{ row }">{{ formatDateTimeDeleteT(formatDate(row.recordTime)) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>

      </el-table>

      <div class="pagination-wrap" v-show="listData">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="listData?.total"
          :page-size="listData?.size" :current-page="listData?.page" :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">

        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" :disabled="!!editingId">
            <el-radio :value="0">支出</el-radio>
            <el-radio :value="1">收入</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.00" :precision="2" style="width: 100%" :max="999999999.99" />
        </el-form-item>

        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in filteredCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="时间" prop="recordTime">
          <el-date-picker v-model="form.recordTime" type="datetime" placeholder="选择时间"
             style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" show-word-limit />
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

.header-actions {
  display: flex;
  gap: 8px;
}

.summary-card {
  text-align: center;
  padding: 4px 0;
}

.summary-label {
  font-size: 14px;
  color: #64748b;
}

.summary-value {
  font-size: 22px;
  font-weight: 700;
  margin-left: 8px;
}

.summary-value.income {
  color: #16a34a;
}

.summary-value.expense {
  color: #dc2626;
}

.filter-card {
  margin-bottom: 16px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
