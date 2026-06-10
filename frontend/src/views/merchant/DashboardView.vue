<template>
  <div class="merchant-orders-page">
    <!-- 头部 -->
    <div class="header-gradient" style="justify-content:space-between;">
      <span style="font-size:18px;">🏪 商家中心</span>
      <span style="font-size:20px;cursor:pointer;" @click="$router.push('/merchant/shop')">⚙️</span>
    </div>

    <!-- 统计卡片 -->
    <div class="mo-stats">
      <div class="mo-stat-item">
        <b>{{ pendingCount }}</b>
        <span>待接单</span>
      </div>
      <div class="mo-stat-item">
        <b>{{ todayCount }}</b>
        <span>今日订单</span>
      </div>
      <div class="mo-stat-item">
        <b>¥{{ todayEarnings }}</b>
        <span>今日收益</span>
      </div>
      <div class="mo-stat-item" style="cursor:pointer;" @click="$router.push('/merchant/reviews')">
        <b>⭐{{ merchant?.rating }}</b>
        <span>店铺评分 ›</span>
      </div>
    </div>

    <!-- 状态筛选 -->
    <div style="background:#FFF;">
      <el-tabs v-model="status" @tab-change="loadData" stretch>
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待接单" name="待接单" />
        <el-tab-pane label="待配送" name="待配送" />
        <el-tab-pane label="配送中" name="配送中" />
        <el-tab-pane label="已完成" name="已完成" />
      </el-tabs>
    </div>

    <!-- 订单列表 -->
    <div class="mo-list">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div v-for="i in 4" :key="'s'+i" class="card" style="margin:0 12px 10px;">
          <div style="display:flex;justify-content:space-between;margin-bottom:10px;">
            <div class="skeleton" style="width:140px;height:14px;"></div>
            <div class="skeleton" style="width:52px;height:22px;border-radius:12px;"></div>
          </div>
          <div class="skeleton" style="width:100%;height:4px;margin-bottom:8px;"></div>
          <div v-for="j in 2" :key="j" class="skeleton" style="width:70%;height:14px;margin-bottom:6px;"></div>
        </div>
      </template>

      <div
        v-for="o in orders"
        :key="o.id"
        class="card mo-order"
        style="margin:0 12px 10px; cursor:pointer;"
        @click="$router.push(`/merchant/order/${o.id}`)"
      >
        <div class="moo-top">
          <span class="moo-order-no">📋 {{ (o.orderNo || '').substring(0, 18) }}...</span>
          <el-tag :type="statusType(o.status)" size="small" round>{{ o.status }}</el-tag>
        </div>

        <!-- 下单时间 -->
        <div class="oc-time">{{ fmtTime(o.createdAt) }}</div>

        <!-- 进度条 -->
        <div class="oc-progress">
          <div class="ocp-track">
            <div class="ocp-fill" :style="{ width: progressPercent(o.status) + '%' }"></div>
          </div>
          <div class="ocp-steps">
            <span :class="{ done: progressStep(o.status) >= 1 }">待接单</span>
            <span :class="{ done: progressStep(o.status) >= 2 }">待配送</span>
            <span :class="{ done: progressStep(o.status) >= 3 }">配送中</span>
            <span :class="{ done: progressStep(o.status) >= 4 }">已完成</span>
          </div>
        </div>

        <template v-for="(it, idx) in (itemsMap[o.id] || [])" :key="it.id">
          <div v-if="idx < showLimit || expandedOrders.has(o.id)" class="moo-item">
            <span>{{ it.productName }}</span>
            <span class="moo-qty">x{{ it.quantity }}</span>
          </div>
        </template>
        <div
          v-if="(itemsMap[o.id] || []).length > showLimit"
          class="oc-expand"
          @click.stop="toggleExpand(o.id)"
        >
          {{ expandedOrders.has(o.id) ? '收起 ▲' : `展开全部（${(itemsMap[o.id] || []).length}件） ▼` }}
        </div>

        <div class="moo-bottom">
          <span>合计 <b>¥{{ o.totalAmount }}</b></span>
          <span class="moo-actions" @click.stop>
            <el-button
              v-if="o.status === '待接单'"
              size="small"
              type="warning"
              @click="handleAction(o.id, 'accept')"
            >接单</el-button>
            <el-button
              v-if="o.status === '待配送'"
              size="small"
              type="primary"
              @click="handleAction(o.id, 'deliver')"
            >配送</el-button>
            <el-button
              v-if="o.status === '配送中'"
              size="small"
              type="success"
              @click="handleAction(o.id, 'complete')"
            >完成</el-button>
          </span>
        </div>
      </div>

      <el-empty v-if="!orders.length" description="暂无订单" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api/merchant'

const loading = ref(true)
const status = ref('')
const orders = ref<any[]>([])
const itemsMap = ref<Record<number, any[]>>({})
const merchant = ref<any>(null)
const pendingCount = ref(0)
const todayCount = ref(0)
const todayEarnings = ref(0)
const showLimit = 3
const expandedOrders = ref(new Set<number>())

function toggleExpand(orderId: number) {
  if (expandedOrders.value.has(orderId)) {
    expandedOrders.value.delete(orderId)
  } else {
    expandedOrders.value.add(orderId)
  }
}

const stepMap: Record<string, number> = {
  '待接单': 1, '待配送': 2, '配送中': 3, '已完成': 4,
}
function progressStep(s: string) {
  return stepMap[s] || 0
}
function progressPercent(s: string) {
  const step = stepMap[s] || 0
  if (step === 0) return 0
  return ((step - 1) / 3) * 100
}

function fmtTime(t: string) {
  if (!t) return ''
  return t.substring(0, 16).replace('T', ' ')
}

const statusTypeMap: Record<string, string> = {
  '待接单': 'warning', '待配送': 'primary',
  '配送中': '', '已完成': 'success',
}
function statusType(s: string) {
  return statusTypeMap[s] || 'info'
}

async function loadData() {
  loading.value = true
  const res: any = await merchantApi.getOrders(status.value || undefined)
  orders.value = res.orders || []
  itemsMap.value = res.itemsMap || {}
  merchant.value = res.merchant
  pendingCount.value = res.pendingCount || 0
  todayCount.value = res.todayCount || 0
  todayEarnings.value = res.todayEarnings || 0
  loading.value = false
}

async function handleAction(id: number, action: string) {
  if (action === 'accept') await merchantApi.acceptOrder(id)
  else if (action === 'deliver') await merchantApi.deliverOrder(id)
  else if (action === 'complete') await merchantApi.completeOrder(id)

  const labels: Record<string, string> = {
    accept: '已接单', deliver: '已开始配送', complete: '已完成',
  }
  ElMessage.success(labels[action] || '操作成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
/* 统计卡片 */
.mo-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 12px 14px;
  background: #FFF;
}

.mo-stat-item {
  text-align: center;
  padding: 10px 4px;
  background: #FAFAFA;
  border-radius: 10px;
  border-top: 3px solid #FFD101;
}

.mo-stat-item b {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #FF6B35;
}

.mo-stat-item span {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
  display: block;
}

/* 订单列表 */
.mo-list {
  padding: 10px 0 0;
}

.moo-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.moo-order-no {
  font-size: 13px;
  color: #999;
}

.oc-time {
  font-size: 11px;
  color: #BBB;
  margin: 2px 0 6px;
}

.moo-item {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 13px;
  color: #666;
}

.moo-qty {
  color: #999;
}

.moo-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #F0F0F0;
  margin-top: 6px;
  font-size: 14px;
}

.moo-bottom b {
  font-size: 15px;
}

.moo-actions {
  display: flex;
  gap: 6px;
}

/* 进度条 */
.oc-progress {
  margin: 4px 0 10px;
}

.ocp-track {
  height: 4px;
  background: #F0F0F0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.ocp-fill {
  height: 100%;
  background: linear-gradient(90deg, #FFD101, #FFB800);
  border-radius: 2px;
  transition: width 0.4s ease;
}

.ocp-steps {
  display: flex;
  justify-content: space-between;
  font-size: 10px;
  color: #CCC;
}

.ocp-steps span.done {
  color: #FFB800;
  font-weight: 600;
}

/* 展开按钮 */
.oc-expand {
  text-align: center;
  padding: 4px 0;
  font-size: 12px;
  color: #FFB800;
  cursor: pointer;
  user-select: none;
}
</style>
