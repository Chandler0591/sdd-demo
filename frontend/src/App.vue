<template>
  <el-container class="app-root">
    <el-aside width="200px" class="app-aside">
      <div class="app-logo">图书管理系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/books">
          <el-icon><Notebook /></el-icon>
          <span>图书管理</span>
        </el-menu-item>
        <el-menu-item index="/readers">
          <el-icon><User /></el-icon>
          <span>读者管理</span>
        </el-menu-item>
        <el-menu-item index="/loans">
          <el-icon><Switch /></el-icon>
          <span>借还办理</span>
        </el-menu-item>
        <el-menu-item index="/shelvings">
          <el-icon><Box /></el-icon>
          <span>上架办理</span>
        </el-menu-item>
        <el-menu-item index="/dicts">
          <el-icon><Collection /></el-icon>
          <span>字典管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main class="app-main">
      <!-- 页签式菜单：已打开页面可切换、可关闭，切换后页面数据保留（keep-alive） -->
      <el-tabs
        v-if="tabs.length > 0"
        v-model="activeTab"
        type="card"
        class="app-tabs"
        closable
        @tab-remove="onTabRemove"
      >
        <el-tab-pane v-for="t in tabs" :key="t.path" :label="t.title" :name="t.path" />
      </el-tabs>
      <div class="app-content">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Box, Collection, Notebook, Switch, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

/** 已打开页签（与路由双向同步） */
const tabs = ref<{ path: string; title: string }[]>([])

const activeTab = computed({
  get: () => route.path,
  set: (path: string) => router.push(path)
})

watch(
  () => route.path,
  (path) => {
    // 空白页等隐藏页签的路由不生成页签
    if (route.meta.hiddenTab) return
    if (!tabs.value.some((t) => t.path === path)) {
      tabs.value.push({ path, title: String(route.meta.title ?? path) })
    }
  },
  { immediate: true }
)

/** 关闭页签：移除后导航到相邻页签（右侧优先），全部关闭则到空白页 */
function onTabRemove(path: string | number) {
  const p = String(path)
  const idx = tabs.value.findIndex((t) => t.path === p)
  if (idx === -1) return
  tabs.value.splice(idx, 1)
  if (route.path === p) {
    const next = tabs.value[idx] ?? tabs.value[idx - 1]
    router.push(next ? next.path : '/blank')
  }
}
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
}

.app-root {
  height: 100%;
}

.app-aside {
  border-right: 1px solid var(--el-border-color);
}

.app-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid var(--el-border-color);
}

.app-main {
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-tabs {
  background: #fff;
  padding: 0 12px;
  border-radius: 4px;
  flex-shrink: 0;
}

.app-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.app-content {
  flex: 1;
  overflow: auto;
  margin-top: 12px;
}
</style>
