<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { financePlanApi } from '@/api/modules/financePlan'
import type { FinancePlan, FinancePlanForm, FinancePlanSummary } from '@/types'
import { formatCurrency, formatRate, profitColor, formatDate } from '@/utils'
import { Plus } from '@element-plus/icons-vue'

//存储后端返回的分页 + 列表 + 统计数据
const listData = ref<{ total: number; page: number; size: number; pages: number; summary: FinancePlanSummary; records: FinancePlan[] } | null>(null)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建理财计划')
const editingId = ref<number | null>(null)//编辑时存储当前行 id
const valuationVisible = ref(false)//更新市值小弹窗显示隐藏。
const valuationId = ref<number | null>(null)//要更新市值的理财计划 id
const valuationValue = ref(0)//弹窗里绑定的最新市值
const statusFilter = ref<0 | 1 | undefined>(undefined)//状态筛选

const formRef = ref<FormInstance>()
//理财计划新增 / 编辑表单响应式对象
const form = reactive<FinancePlanForm>({
  name: '',
  initialAmount: 0,
  currentValue: 0,
  expectedRoi: undefined,
  startDate: '',
  endDate: '',
  remark: '',
})

const isEditMode = ref(false)//标记是否为编辑模式

const formRules = computed<FormRules>(() => ({
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  initialAmount: [{ required: true, message: '请输入初始投入金额', trigger: 'blur' }],
  currentValue: isEditMode.value
    ? []
    : [{ required: true, message: '请输入当前市值', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
}))

//获取理财计划列表
async function fetchPlans(page = 1) {
  loading.value = true
  try {
    listData.value = await financePlanApi.getPlans({ status: statusFilter.value, page, size: 10 })
  } finally {
    loading.value = false
  }
}
//打开新建弹窗
function openCreateDialog() {
  isEditMode.value = false
  dialogTitle.value = '新建理财计划'
  editingId.value = null
  form.name = ''
  form.initialAmount = 0
  form.currentValue = 0
  form.expectedRoi = undefined
  form.startDate = ''
  form.endDate = ''
  form.remark = ''
  dialogVisible.value = true
}
//打开编辑弹窗
function openEditDialog(row: FinancePlan) {
  isEditMode.value = true
  dialogTitle.value = '修改理财计划'
  editingId.value = row.id
  // 把当前行数据赋值给表单，回显
  form.name = row.name
  form.initialAmount = row.initialAmount
  form.currentValue = row.currentValue
  form.expectedRoi = row.expectedRoi ?? undefined
  form.startDate = row.startDate
  form.endDate = row.endDate || ''
  form.remark = row.remark || ''
  dialogVisible.value = true
}

const submitLoading = ref(false)//提交按钮 loading 防止重复点击
//表单提交
async function handleSubmit() {
  if (!formRef.value) {
    ElMessage.warning('表单未就绪，请稍后再试')
    return
  }
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (editingId.value) {
      // 编辑
      await financePlanApi.updatePlan(editingId.value, {
        name: form.name,
        initialAmount: form.initialAmount,
        currentValue: form.currentValue,
        expectedRoi: form.expectedRoi,
        remark: form.remark,
        startDate: form.startDate,
        endDate: form.endDate || undefined,
      })
      ElMessage.success('更新成功')
    } else {
      // 新建
      await financePlanApi.createPlan({ ...form })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchPlans()
  } catch { } finally {
    submitLoading.value = false
  }
}
//删除理财计划
async function handleDelete(id: number) {
  await ElMessageBox.confirm('确定要删除该理财计划吗？', '确认删除', { type: 'warning' })
  try {
    await financePlanApi.deletePlan(id)
    ElMessage.success('删除成功')
    fetchPlans()
  } catch { }
}
//打开更新市值弹窗，回显当前市值、记录 id
function openValuationDialog(row: FinancePlan) {
  valuationId.value = row.id
  valuationValue.value = row.currentValue
  valuationVisible.value = true
}
//提交更新市值
async function handleValuation() {
  if (!valuationId.value) return
  try {
    await financePlanApi.updateValuation(valuationId.value, { currentValue: valuationValue.value })
    ElMessage.success('市值更新成功')
    valuationVisible.value = false
    fetchPlans()
  } catch { }
}
//修改状态为已赎回
async function handleRedeem(row: FinancePlan) {
  await ElMessageBox.confirm('确定将该计划标记为已赎回吗？', '确认赎回', { type: 'info' })
  try {
    await financePlanApi.updatePlanStatus(row.id, { status: 1 })
    ElMessage.success('已标记为已赎回')
    fetchPlans()
  } catch { }
}

onMounted(() => fetchPlans())
</script>

<template>
  <div class="plan-page">
    <div class="page-header">
      <h3 class="page-title">理财计划</h3>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新建计划</el-button>
    </div>

    <!-- Summary Cards -->
    <el-row :gutter="20" style="margin-bottom: 16px" v-if="listData?.summary">
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总投资</div>
          <div class="stat-value">{{ formatCurrency(listData.summary.totalInvested) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">当前市值</div>
          <div class="stat-value blue">{{ formatCurrency(listData.summary.totalCurrentValue) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总盈亏</div>
          <div class="stat-value" :style="{ color: profitColor(listData.summary.totalProfit) }">
            {{ formatCurrency(listData.summary.totalProfit) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">总收益率</div>
          <div class="stat-value" :style="{ color: profitColor(listData.summary.overallProfitRate) }">
            {{ formatRate(listData.summary.overallProfitRate) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-radio-group v-model="statusFilter" @change="fetchPlans()">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">持有中</el-radio-button>
        <el-radio-button :value="1">已赎回</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="listData?.records || []" v-loading="loading" stripe>

        <el-table-column prop="name" label="计划名称" min-width="120" show-overflow-tooltip />

        <el-table-column prop="initialAmount" label="初始投入" width="120">
          <template #default="{ row }">{{ formatCurrency(row.initialAmount) }}</template>
        </el-table-column>

        <el-table-column prop="currentValue" label="当前市值" width="120">
          <template #default="{ row }">{{ formatCurrency(row.currentValue) }}</template>
        </el-table-column>

        <el-table-column label="盈亏" width="140">
          <template #default="{ row }">
            <span :style="{ color: profitColor(row.profitAmount) }">
              {{ formatCurrency(row.profitAmount) }} ({{ formatRate(row.profitRate) }})
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="expectedRoi" label="预期年化" width="90">
          <template #default="{ row }">{{ row.expectedRoi != null ? row.expectedRoi + '%' : '-' }}</template>
        </el-table-column>

        <el-table-column prop="startDate" label="开始日期" width="100" />

        <el-table-column prop="endDate" label="结束日期" width="100">
          <template #default="{ row }">{{ row.endDate || '-' }}</template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'info'" effect="light">
              {{ row.status === 0 ? '持有中' : '已赎回' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="warning" link @click="openValuationDialog(row)">更新市值</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="handleRedeem(row)">赎回</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>

      </el-table>

      <div class="pagination-wrap" v-if="listData">
        <el-pagination background layout="total, prev, pager, next" :total="listData.total" :page-size="listData.size"
          :current-page="listData.page" @current-change="fetchPlans" />
      </div>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="计划名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="初始投入" prop="initialAmount">
          <el-input-number v-model="form.initialAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前市值" prop="currentValue">
          <el-input-number v-model="form.currentValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预期年化(%)" prop="expectedRoi">
          <el-input-number v-model="form.expectedRoi" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" placeholder="可不填" value-format="YYYY-MM-DD"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Valuation Dialog -->
    <el-dialog v-model="valuationVisible" title="更新市值" width="360px">
      <el-form label-width="80px">
        <el-form-item label="最新市值">
          <el-input-number v-model="valuationValue" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="valuationVisible = false">取消</el-button>
        <el-button type="primary" @click="handleValuation">确定</el-button>
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

.stat-card {
  text-align: center;
  padding: 4px 0;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #334155;
}

.stat-value.blue {
  color: #2563eb;
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
