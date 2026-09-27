import { defineStore } from 'pinia'
import { ref } from 'vue'
//管理侧边栏的展开 / 收起状态

export const useAppStore = defineStore('app', () => {
  //侧边栏是否收起
  const sidebarCollapsed = ref(false)

  //切换方法
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return { sidebarCollapsed, toggleSidebar }
})
