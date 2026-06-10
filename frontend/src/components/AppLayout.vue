<template>
  <div class="page" :class="{ 'page-no-nav': hideNav }">
    <router-view />
    <!-- 底部导航栏 -->
    <div v-if="!hideNav" class="bottom-nav">
      <div
        v-for="t in tabs"
        :key="t.key"
        class="bottom-nav-item"
        :class="{ active: $route.path.startsWith(t.path) }"
        @click="$router.push(t.path)"
      >
        <el-icon :size="22"><component :is="t.icon" /></el-icon>
        <span>{{ t.label }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled, Present, Tickets, User } from '@element-plus/icons-vue'

const route = useRoute()
const hideNav = computed(() =>
  route.path.includes('/consumer/merchant/') ||
  route.path.includes('/consumer/checkout/')
)

const tabs = [
  { key: 'home',    label: '首页', path: '/consumer/home',     icon: HomeFilled },
  { key: 'coupons', label: '神券', path: '/consumer/coupons',  icon: Present },
  { key: 'orders',  label: '订单', path: '/consumer/orders',   icon: Tickets },
  { key: 'my',      label: '我的', path: '/consumer/profile',  icon: User },
]
</script>
