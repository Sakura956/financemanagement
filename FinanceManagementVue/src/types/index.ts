// ========== 通用 API 类型 ==========

//后端返回的统一格式
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}
//分页数据格式
export interface PageResult<T> {
  total: number
  page: number
  size: number
  pages: number
  records: T[]
}
//分页参数
export interface PageParams {
  page?: number
  size?: number
  sortBy?: string
  order?: 'asc' | 'desc'
}

// ========== 权限/用户类型 ==========

// 用户信息：ID、手机号、昵称、角色、头像、状态、创建时间
export interface UserInfo {
  id: number
  phone: string
  nickname: string
  role: 'USER' | 'ADMIN'
  avatarUrl: string | null
  status?: number
  createTime?: string
  updateTime?: string
  billCount?: number
  memoCount?: number
  planCount?: number
}
//登录提交参数
export interface LoginRequest {
  phone: string
  password: string
}
//登录返回参数
export interface LoginResult {
  token: string
  userInfo: UserInfo
}
//注册提交参数
export interface RegisterRequest {
  phone: string
  password: string
  nickname?: string
}
//修改密码提交参数
export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}
//修改个人信息提交参数
export interface UpdateProfileRequest {
  nickname?: string
  avatarUrl?: string
}
export interface UpdateAvatarRequest {
  file:File
}


// ========== 账单相关 ==========

//账单：收入 / 支出、金额、分类、备注、时间
export interface Bill {
  id: number
  type: 0 | 1 // 0-支出 1-收入
  amount: number
  categoryId: number
  categoryName: string
  categoryIcon: string
  description: string | null
  recordTime: string
  createTime?: string
  updateTime?: string
}
//账单查询条件：类型、分类、时间范围
export interface BillQuery extends PageParams {
  type?: 0 | 1
  categoryId?: number
  startDate?: string
  endDate?: string
  keyword?: string
  minAmount?: number
  maxAmount?: number
}
// 添加账单表单
export interface BillForm {
  type: 0 | 1
  amount: number
  categoryId: number
  description?: string
  recordTime: string
}
//修改账单的表单
export interface BillUpdateForm {
  amount?: number
  categoryId?: number
  description?: string
  recordTime?: string
}
//账单查询返回
export interface BillListData extends PageResult<Bill> {
  summary: {
    totalIncome: number
    totalExpense: number
  }
}

// ========== 账单分类类型 ==========

//分类：名称、类型、图标、排序、状态
export interface Category {
  id: number
  name: string
  type: 0 | 1
  icon: string | null
  sortOrder: number
  isDefault: number
  status: number
  billCount?: number
}

export interface CategoryForm {
  name: string
  type: 0 | 1
  icon?: string
  sortOrder?: number
}

// ========== 理财计划类型 ==========

//理财：名称、本金、当前价值、收益、收益率、时间、状态  
export interface FinancePlan {
  id: number
  name: string
  initialAmount: number
  currentValue: number
  profitAmount: number
  profitRate: number
  expectedRoi: number | null
  startDate: string
  endDate: string | null
  status: 0 | 1 // 0-持有中 1-已赎回
  remark: string | null
  createTime: string
  updateTime: string
}

export interface FinancePlanSummary {
  totalInvested: number
  totalCurrentValue: number
  totalProfit: number
  overallProfitRate: number
}

export interface FinancePlanListData extends PageResult<FinancePlan> {
  summary: FinancePlanSummary
}

export interface FinancePlanForm {
  name: string
  initialAmount: number
  currentValue: number
  expectedRoi?: number
  startDate: string
  endDate?: string
  remark?: string
}

export interface ValuationForm {
  currentValue: number
}

export interface PlanStatusForm {
  status: 1
  endDate?: string
}

// ========== 统计模块（图表、概览） ==========

//月度统计：收入、支出、结余
export interface StatisticsOverview {
  month: string
  income: number
  expense: number
  balance: number
}

export interface CategoryPieItem {
  categoryId: number
  categoryName: string
  categoryIcon: string
  amount: number
  percent: string
  count: number
}
// 饼图数据：分类、金额、占比
export interface CategoryPieData {
  totalAmount: number
  items: CategoryPieItem[]
}

export interface TrendItem {
  month: string
  income: number
  expense: number
  balance: number
}

export interface YearlySummary {
  year: number
  totalIncome: number
  totalExpense: number
  balance: number
  monthlyAvgExpense: number
  highestExpenseMonth: string
  highestExpenseAmount: number
  lowestExpenseMonth: string
  lowestExpenseAmount: number
}

// ========== 备忘录类型 ==========

//备忘录：标题、内容、提醒时间、是否完成
export interface Memo {
  id: number
  title: string
  content: string | null
  remindTime: string | null
  isCompleted: 0 | 1
  createTime: string
  updateTime: string
}

export interface MemoForm {
  title: string
  content?: string
  remindTime?: string
}

// ========== AI 对话类型（聊天、诊断、建议） ==========
export interface AISession {
  sessionId: string
  title: string
  lastMessage: string
  messageCount: number
  lastActiveTime: string
  createTime: string
}

export interface AIMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  tokensUsed: number
  createTime: string
}

export interface ChatRequest {
  sessionId?: string
  message: string
  includeHistory?: boolean
}

export interface ChatResult {
  sessionId: string
  message: string
  tokensUsed: number
  createdAt: string
}

export interface SSEToken {
  token: string
  sessionId?: string
  tokensUsed?: number
  finished?: boolean
}

export interface FinanceDiagnosisRequest {
  month?: string
  analysisType?: 'overall' | 'spending' | 'saving'
}

export interface FinanceDiagnosisResult {
  sessionId: string
  analysisType: string
  month: string
  dataSummary: {
    income: number
    expense: number
    balance: number
    top3ExpenseCategories: { name: string; amount: number; percent: string }[]
  }
  aiAdvice: string
}

export interface InvestmentAdviceResult {
  sessionId: string
  plansSummary: {
    totalPlans: number
    totalInvested: number
    totalCurrentValue: number
    overallProfitRate: number
  }
  aiAdvice: string
}

// ========== 管理员类型 ==========
export interface AdminDashboard {
  totalUsers: number
  activeUsersToday: number
  newUsersThisWeek: number
  newUsersThisMonth: number
  totalBills: number
  billsToday: number
  totalPlans: number
  totalMemos: number
}

export interface UserQuery extends PageParams {
  keyword?: string
  status?: 0 | 1
  startDate?: string
  endDate?: string
}
