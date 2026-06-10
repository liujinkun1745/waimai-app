<template>
  <div class="home-page fade-in-up">
    <!-- 顶部黄色搜索区 -->
    <div class="home-header">
      <div class="home-header-row">
        <span class="home-city">📍 北京</span>
        <div class="home-search" @click="$router.push('/consumer/search')">
          <el-icon :size="16" color="#999"><Search /></el-icon>
          <span>搜索商家或商品</span>
        </div>
      </div>
    </div>

    <!-- 8个分类图标行 -->
    <div class="home-categories card" style="margin-top: -8px; position: relative;">
      <div
        v-for="c in categories"
        :key="c.key"
        class="home-cate-item"
        :class="{ on: activeCat === c.key }"
        @click="toggleCategory(c.key)"
      >
        <div class="home-cate-icon" :class="{ active: activeCat === c.key }">{{ c.emoji }}</div>
        <span class="home-cate-label">{{ c.label }}</span>
      </div>
    </div>

    <!-- 排序栏 -->
    <div class="home-sort-bar">
      <span class="home-sort-title">
        <template v-if="activeCat">🔍 {{ catLabel }}</template>
        <template v-else>🔥 附近商家</template>
      </span>
      <span v-if="activeCat" class="home-clear-filter" @click="toggleCategory(activeCat)">✕ 清除</span>
      <el-radio-group v-model="sort" size="small" @change="loadMerchants">
        <el-radio-button value="sales">销量优先</el-radio-button>
        <el-radio-button value="rating">评分优先</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 商家卡片列表 -->
    <div class="home-merchant-list">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div v-for="i in 5" :key="'s'+i" class="card" style="margin:0 12px 10px;">
          <div style="display:flex;gap:12px;">
            <div class="skeleton" style="width:64px;height:64px;border-radius:8px;flex-shrink:0;"></div>
            <div style="flex:1;display:flex;flex-direction:column;gap:8px;">
              <div class="skeleton" style="width:60%;height:18px;"></div>
              <div class="skeleton" style="width:40%;height:14px;"></div>
              <div class="skeleton" style="width:80%;height:12px;"></div>
            </div>
          </div>
        </div>
      </template>

      <div
        v-for="m in merchants"
        :key="m.id"
        class="home-merchant-card card"
        style="margin: 0 12px 10px; cursor: pointer;"
        @click="$router.push(`/consumer/merchant/${m.id}`)"
      >
        <div class="hmc-row">
          <div class="hmc-avatar">{{ getEmoji(m.shopName) }}</div>
          <div class="hmc-body">
            <div class="hmc-name">{{ m.shopName }}</div>
            <div class="hmc-meta">
              <span class="hmc-rating">⭐ {{ m.rating }}</span>
              <span>月售 {{ orderCounts[m.id] || m.monthlySales }}</span>
            </div>
            <div class="hmc-info">
              <span>¥{{ m.deliveryFee }} 配送</span>
              <span class="hmc-dot">·</span>
              <span>起送 ¥{{ m.minOrderAmount }}</span>
              <span class="hmc-dot">·</span>
              <span>{{ m.businessHours }}</span>
            </div>
            <div class="hmc-desc" v-if="m.description">{{ truncate(m.description, 20) }}</div>
          </div>
          <el-tag
            :type="m.status === '营业中' ? 'success' : 'info'"
            size="small"
            round
            class="hmc-status"
          >
            {{ m.status }}
          </el-tag>
        </div>
      </div>

      <el-empty v-if="!merchants.length" description="暂无商家 😢" />
    </div>

    <!-- 底部留白 -->
    <div style="height: 8px;"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { consumerApi } from '@/api/consumer'

const loading = ref(true)
const sort = ref('sales')
const merchants = ref<any[]>([])
const orderCounts = ref<Record<number, number>>({})
const activeCat = ref('')

const categories = [
  { emoji: '🍜', label: '家常菜', key: 'jiacai' },
  { emoji: '🍰', label: '烘焙', key: 'hongbei' },
  { emoji: '🍎', label: '鲜果', key: 'fruit' },
  { emoji: '🍔', label: '汉堡', key: 'hanbao' },
  { emoji: '🍖', label: '烧烤', key: 'bbq' },
  { emoji: '☕', label: '咖啡', key: 'coffee' },
  { emoji: '🏪', label: '超市', key: 'market' },
  { emoji: '🎂', label: '西饼', key: 'cake' },
]

const catLabel = computed(() => categories.find(c => c.key === activeCat.value)?.label || '')

const emojiMap: Record<string, string> = {
  '美味家常菜': '🍜', '甜蜜时光烘焙': '🍰', '鲜果鲜生': '🍎',
  '深夜烧烤': '🍖', '瑞幸咖啡': '☕', '麦香鸡汉堡': '🍔',
  '幸福西饼': '🎂', '天天超市': '🏪',
}

function getEmoji(name: string) {
  return emojiMap[name] || '🏪'
}

function truncate(s: string, max: number) {
  return s.length > max ? s.substring(0, max) + '…' : s
}

function toggleCategory(key: string) {
  activeCat.value = activeCat.value === key ? '' : key
  loadMerchants()
}

async function loadMerchants() {
  loading.value = true
  const keyword = activeCat.value ? catLabel.value : undefined
  const res: any = await consumerApi.getHome(keyword, sort.value)
  merchants.value = res.merchants || []
  orderCounts.value = res.monthlyOrderCounts || {}
  loading.value = false
}

onMounted(loadMerchants)
</script>

<style scoped>
/* 顶部搜索区 */
.home-header {
  background: linear-gradient(180deg, #FFD101 0%, #FFD101 55%, #FFF8E1 100%);
  padding: 14px 14px 22px;
}

.home-header-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-city {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.home-search {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  background: #FFF;
  border-radius: 20px;
  padding: 10px 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  cursor: pointer;
}

.home-search span {
  font-size: 14px;
  color: #BBB;
}

/* 分类图标行 */
.home-categories {
  display: flex;
  justify-content: space-around;
  padding: 16px 8px 12px;
}

.home-cate-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: transform 0.15s;
}

.home-cate-item:active {
  transform: scale(0.92);
}

.home-cate-item.on .home-cate-label {
  color: #FFB800;
  font-weight: 700;
}

.home-cate-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #FFF8E1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.2s;
}

.home-cate-icon.active {
  background: #FFB800;
  transform: scale(1.08);
}

.home-cate-label {
  font-size: 11px;
  color: #666;
  transition: color 0.15s;
}

/* 排序栏 */
.home-sort-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 14px 4px;
  flex-wrap: wrap;
  gap: 4px;
}

.home-sort-title {
  font-size: 16px;
  font-weight: 700;
}

.home-clear-filter {
  font-size: 12px;
  color: #FF6B35;
  cursor: pointer;
  padding: 2px 10px;
  border-radius: 12px;
  background: #FFF3CD;
  flex-shrink: 0;
}

/* 商家列表 */
.home-merchant-list {
  padding: 4px 0 0;
}

/* 商家卡片内部 */
.hmc-row {
  display: flex;
  gap: 12px;
  position: relative;
}

.hmc-avatar {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background: #FFF8E1;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}

.hmc-body {
  flex: 1;
  min-width: 0;
}

.hmc-name {
  font-size: 16px;
  font-weight: 700;
  margin-bottom: 4px;
}

.hmc-meta {
  font-size: 13px;
  color: #666;
  margin-bottom: 2px;
}

.hmc-rating {
  color: #FFB800;
  font-weight: 600;
  margin-right: 6px;
}

.hmc-info {
  font-size: 12px;
  color: #999;
}

.hmc-dot {
  margin: 0 4px;
  color: #DDD;
}

.hmc-desc {
  font-size: 12px;
  color: #FF6B35;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hmc-status {
  position: absolute;
  top: 0;
  right: 0;
}
</style>
