/**
 * 格式化金额：把数字变成 人民币格式
 * 1234.5 → ¥1234.50
 */
export function formatCurrency(value: number, decimals = 2): string {
  return `¥${value.toFixed(decimals)}`
}

/**
 * 数字格式化（千分位）
 * 数字加逗号分隔：123456.78 → 123,456.78
 */
export function formatNumber(value: number, decimals = 2): string {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })
}

/**
 *  简单日期格式化
 */
export function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  // Handles both "yyyy-MM-dd" and "yyyy-MM-dd HH:mm:ss"
  return dateStr.replace(' ', ' ').trim()
}

/**
 * ISO 时间 → 显示日期
 */
export function toDisplayDate(isoStr: string): string {
  if (!isoStr) return ''
  const d = new Date(isoStr)
  if (isNaN(d.getTime())) return isoStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 获取今天日期
 */
export function today(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 获取当前完整时间
 * 返回：2026-05-11 19:50:30
 */
export function now(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${mi}:${s}`
}

/**
 * 格式时间2026-05-12T18:50:27，中间不要T
 * @param date 
 * @returns 
 */
export function formatDateTimeDeleteT(date: string | Date) {
  return new Date(date).toISOString().slice(0, 19).replace('T', ' ')
}

/**
 * 格式化收益率（带正负号）
 * 5.2 → +5.20%
 */
export function formatRate(rate: number): string {
  const sign = rate >= 0 ? '+' : ''
  return `${sign}${rate.toFixed(2)}%`
}

/**
 * 收益颜色（红涨绿跌）
 */
export function profitColor(rate: number): string {
  if (rate > 0) return '#16a34a'
  if (rate < 0) return '#dc2626'
  return '#6b7280'
}
