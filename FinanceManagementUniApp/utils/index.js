// 通用工具函数

/**
 * 格式化金额（保留两位小数，添加千分位分隔）
 * @param {Number} value - 金额
 * @returns {String}
 */
export function formatMoney(value) {
  if (value === null || value === undefined) return '0.00'
  const num = Number(value)
  if (isNaN(num)) return '0.00'
  return num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 格式化金额为简写（大额显示万）
 * @param {Number} value - 金额
 * @returns {String}
 */
export function formatMoneyShort(value) {
  if (value === null || value === undefined) return '0'
  const num = Number(value)
  if (isNaN(num)) return '0'
  if (Math.abs(num) >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toFixed(0)
}

/**
 * 格式化日期
 * @param {String|Number} date - 日期字符串或时间戳
 * @param {String} format - 格式，默认 yyyy-MM-dd
 * @returns {String}
 */
export function formatDate(date, format = 'yyyy-MM-dd') {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''

  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('yyyy', year)
    .replace('MM', month)
    .replace('dd', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 获取当前月份（yyyy-MM 格式）
 * @returns {String}
 */
export function getCurrentMonth() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

/**
 * 获取当前年份
 * @returns {Number}
 */
export function getCurrentYear() {
  return new Date().getFullYear()
}

/**
 * 防抖
 * @param {Function} fn - 要防抖的函数
 * @param {Number} delay - 延迟毫秒数
 * @returns {Function}
 */
export function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 手机号校验
 * @param {String} phone - 手机号
 * @returns {Boolean}
 */
export function validatePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 密码校验（6-20位，至少包含字母和数字）
 * @param {String} password - 密码
 * @returns {Boolean}
 */
export function validatePassword(password) {
  if (!password || password.length < 6 || password.length > 20) return false
  return /[a-zA-Z]/.test(password) && /\d/.test(password)
}
