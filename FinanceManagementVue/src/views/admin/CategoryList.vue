<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { Category, CategoryForm } from '@/types'
import { Plus } from '@element-plus/icons-vue'

const categories = ref<Category[]>([])// 分类列表
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const editingId = ref<number | null>(null)// 编辑ID

const typeFilter = ref<0 | 1 | undefined>(undefined)// 筛选：全部/支出/收入

const formRef = ref<FormInstance>()
const form = reactive<CategoryForm>({ name: '', type: 0, icon: '', sortOrder: 0 })

const formRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }, { min: 2, max: 10, message: '2-10个字符', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
}
//获取分类列表
async function fetchCategories() {
  loading.value = true
  try {
    categories.value = await adminApi.getCategories({
      type: typeFilter.value,// 筛选类型
      includeDisabled: true,// 显示禁用的分类
    })
  } finally {
    loading.value = false
  }
}
//打开新增弹窗
function openCreateDialog() {
  dialogTitle.value = '新增分类'
  editingId.value = null
  form.name = ''
  form.type = 0
  form.icon = ''
  form.sortOrder = 0
  dialogVisible.value = true
}
//打开编辑弹窗
function openEditDialog(row: Category) {
  dialogTitle.value = '修改分类'
  editingId.value = row.id
  form.name = row.name
  form.type = row.type
  form.icon = row.icon || ''
  form.sortOrder = row.sortOrder
  dialogVisible.value = true
}
//提交
async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    if (editingId.value) {
      await adminApi.updateCategory(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await adminApi.createCategory({ ...form })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchCategories()
  } catch {  }
}
//删除分类
async function handleDelete(row: Category) {
  if (row.isDefault === 1) {
    ElMessage.warning('系统默认分类不可删除')
    return
  }
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '确认删除', { type: 'warning' })
    await adminApi.deleteCategory(row.id)
    ElMessage.success('删除成功')
    fetchCategories()
  } catch {  }
}
//启用 / 禁用分类
async function toggleStatus(row: Category) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该分类吗？`, `确认${action}`, { type: 'warning' })
  try {
    await adminApi.updateCategoryStatus(row.id, newStatus as 0 | 1)
    ElMessage.success(`已${action}`)
    fetchCategories()
  } catch {  }
}

onMounted(fetchCategories)
</script>

<template>
  <div class="category-page">
    <div class="page-header">
      <h3 class="page-title">分类管理</h3>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增分类</el-button>
    </div>

    <!-- Filter -->
    <el-card shadow="never" class="filter-card">
      <el-radio-group v-model="typeFilter" @change="fetchCategories()">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">支出分类</el-radio-button>
        <el-radio-button :value="1">收入分类</el-radio-button>
      </el-radio-group>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="categories" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />

        <el-table-column prop="name" label="分类名称" width="150" />

        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 0 ? 'danger' : 'success'" effect="light">
              {{ row.type === 0 ? '支出' : '收入' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="icon" label="图标" width="120" />

        <el-table-column prop="sortOrder" label="排序" width="100" />

        <el-table-column prop="isDefault" label="默认" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.isDefault ? 'info' : ''" effect="light">
              {{ row.isDefault ? '系统' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="light">
              {{ row.status === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="billCount" label="关联账单" width="100">
          <template #default="{ row }">{{ row.billCount ?? '-' }}</template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button :type="row.status === 1 ? 'success' : 'warning'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-button>
            <el-button type="danger" link :disabled="row.isDefault === 1" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="440px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="10" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" :disabled="!!editingId">
            <el-radio :value="0">支出</el-radio>
            <el-radio :value="1">收入</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <el-input v-model="form.icon" placeholder="图标标识（选填）" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
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
</style>
