<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/modules/admin'
import type { UserInfo, UserQuery, PageResult } from '@/types'
import { formatDate } from '@/utils'
import { Search } from '@element-plus/icons-vue'

const listData = ref<PageResult<UserInfo> | null>(null)// 用户列表+分页
const drawerVisible = ref(false)// 详情抽屉
const detailUser = ref<UserInfo | null>(null)// 详情数据
const loading = ref(false)
const detailLoading = ref(false) // 详情加载
//查询参数
const query = reactive<UserQuery>({
  page: 1, size: 10, keyword: '', status: undefined, startDate: '', endDate: '',
})
//获取用户列表
async function fetchUsers() {
  loading.value = true
  try {
    listData.value = await adminApi.getUsers({ ...query })
  } finally {
    loading.value = false
  }
}
//重置查询
function resetQuery() {
  query.page = 1
  fetchUsers()
}
// 查看用户详情（右侧抽屉）
async function viewDetail(userId: number) {
  detailLoading.value = true
  drawerVisible.value = true
  try {
    detailUser.value = await adminApi.getUserDetail(userId)
  } finally {
    detailLoading.value = false
  }
}
// 封禁/解封用户
async function toggleStatus(row: UserInfo) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '封禁' : '解封'
  await ElMessageBox.confirm(`确定要${action}该用户吗？`, `确认${action}`, { type: 'warning' })
  try {
    await adminApi.updateUserStatus(row.id, newStatus as 0 | 1)
    ElMessage.success(`已${action}`)
    fetchUsers()
  } catch { }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="user-list-page">
    <h3 class="page-title">用户管理</h3>

    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form inline :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="手机号/昵称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="正常" :value="0" />
            <el-option label="封禁" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="注册日期">
          <el-date-picker v-model="query.startDate" type="date" placeholder="开始" value-format="YYYY-MM-DD"
            style="width: 140px" />
          <span style="margin: 0 6px">至</span>
          <el-date-picker v-model="query.endDate" type="date" placeholder="结束" value-format="YYYY-MM-DD"
            style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="resetQuery">搜索</el-button>
          <el-button
            @click="query.keyword = ''; query.status = undefined; query.startDate = ''; query.endDate = ''; resetQuery()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never">
      <el-table :data="listData?.records || []" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />

        <el-table-column prop="phone" label="手机号" width="150" />

        <el-table-column prop="nickname" label="昵称" width="140" />

        <el-table-column prop="avatarUrl" label="头像" width="120">
          <template #default="{ row }">
            <el-avatar :src="row.avatarUrl" icon="UserFilled" :size="40" />
          </template>
        </el-table-column>

        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" effect="light">
              {{ row.status === 0 ? '正常' : '封禁' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="注册时间" width="260">
          <template #default="{ row }">{{ formatDate(row.createTime || '') }}</template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">详情</el-button>
            <el-button :type="row.status === 1 ? 'success' : 'danger'" link @click="toggleStatus(row)">
              {{ row.status === 1 ? '解封' : '封禁' }}
            </el-button>
          </template>
        </el-table-column>

      </el-table>

      <div class="pagination-wrap" v-if="listData">
        <el-pagination background layout="total, prev, pager, next" :total="listData.total" :page-size="listData.size"
          :current-page="listData.page" @current-change="(p: number) => { query.page = p; fetchUsers() }" />
      </div>
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="用户详情" size="400px">
      <div v-loading="detailLoading" v-if="detailUser">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>

          <el-descriptions-item label="头像">
            <el-avatar :src="detailUser.avatarUrl" icon="UserFilled" :size="50" />
          </el-descriptions-item>

          <el-descriptions-item label="手机号">{{ detailUser.phone }}</el-descriptions-item>

          <el-descriptions-item label="昵称">{{ detailUser.nickname }}</el-descriptions-item>

          <el-descriptions-item label="角色">
            <el-tag :type="detailUser.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ detailUser.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="状态">
            <el-tag :type="detailUser.status === 0 ? 'success' : 'danger'" effect="light">
              {{ detailUser.status === 0 ? '正常' : '封禁' }}
            </el-tag>
          </el-descriptions-item>

          <el-descriptions-item label="账单数">{{ detailUser.billCount }}</el-descriptions-item>
          <el-descriptions-item label="备忘录数">{{ detailUser.memoCount }}</el-descriptions-item>
          <el-descriptions-item label="理财计划数">{{ detailUser.planCount }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatDate(detailUser.createTime || '') }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDate(detailUser.updateTime || '') }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  color: #1e293b;
  margin: 0 0 16px;
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
