<template>
  <div class="page">
    <router-view />
    <!-- 底部导航栏 -->
    <div class="bottom-nav">
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
import { Tickets, Goods, DataAnalysis, ChatDotRound } from '@element-plus/icons-vue'

const tabs = [
  { key: 'orders',   label: '订单', path: '/merchant/orders',   icon: Tickets },
  { key: 'products', label: '商品', path: '/merchant/products', icon: Goods },
  { key: 'reviews',  label: '评价', path: '/merchant/reviews',  icon: ChatDotRound },
  { key: 'earnings', label: '收益', path: '/merchant/earnings', icon: DataAnalysis },
]
</script>
