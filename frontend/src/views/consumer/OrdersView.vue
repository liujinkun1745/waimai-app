<template>
  <div class="orders-page">
    <!-- 头部 -->
    <div class="header-gradient" style="font-size:18px;">📋 我的订单</div>

    <!-- 状态筛选标签 -->
    <div class="orders-tabs">
      <el-tabs v-model="status" @tab-change="loadOrders" stretch>
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待接单" name="待接单" />
        <el-tab-pane label="待配送" name="待配送" />
        <el-tab-pane label="配送中" name="配送中" />
        <el-tab-pane label="已完成" name="已完成" />
      </el-tabs>
    </div>

    <!-- 订单列表 -->
    <div class="orders-list">
      <!-- 骨架屏 -->
      <template v-if="loading">
        <div v-for="i in 4" :key="'s'+i" class="card" style="margin:0 12px 10px;">
          <div style="display:flex;justify-content:space-between;margin-bottom:10px;">
            <div class="skeleton" style="width:100px;height:18px;"></div>
            <div class="skeleton" style="width:52px;height:22px;border-radius:12px;"></div>
          </div>
          <div class="skeleton" style="width:100%;height:4px;margin-bottom:8px;"></div>
          <div v-for="j in 2" :key="j" class="skeleton" style="width:70%;height:14px;margin-bottom:6px;"></div>
        </div>
      </template>

      <div
        v-for="o in orders"
        :key="o.id"
        class="card orders-card"
        style="margin:0 12px 10px; cursor:pointer;"
        @click="$router.push(`/consumer/order/${o.id}`)"
      >
        <!-- 商家 + 状态 -->
        <div class="oc-top">
          <span class="oc-shop">🏪 {{ o.merchant?.shopName }}</span>
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

        <!-- 商品列表 -->
        <template v-for="(it, idx) in (itemsMap[o.id] || [])" :key="it.id">
          <div v-if="idx < showLimit || expandedOrders.has(o.id)" class="oc-item">
            <span>{{ it.productName }}</span>
            <span class="oc-qty">x{{ it.quantity }}</span>
          </div>
        </template>
        <div
          v-if="(itemsMap[o.id] || []).length > showLimit"
          class="oc-expand"
          @click.stop="toggleExpand(o.id)"
        >
          {{ expandedOrders.has(o.id) ? '收起 ▲' : `展开全部（${(itemsMap[o.id] || []).length}件） ▼` }}
        </div>

        <!-- 底部合计 + 操作 -->
        <div class="oc-bottom">
          <span>
            共{{ (itemsMap[o.id] || []).length }}件 ·
            <b>¥{{ o.totalAmount }}</b>
          </span>
          <span class="oc-actions" @click.stop>
            <el-button
              v-if="o.status === '待接单'"
              size="small"
              type="danger"
              plain
              @click="cancelOrder(o.id)"
            >取消</el-button>
            <el-button
              v-if="o.status === '配送中'"
              size="small"
              type="success"
              @click="confirmReceived(o.id)"
            >确认收货</el-button>
            <el-button
              v-if="o.status === '已完成' && !reviewedMap[o.id]"
              size="small"
              type="warning"
              @click="$router.push(`/consumer/order/${o.id}/review`)"
            >去评价</el-button>
          </span>
        </div>
      </div>

      <el-empty v-if="!orders.length" description="还没有订单，去逛逛吧～" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { consumerApi } from '@/api/consumer'

const loading = ref(true)
const status = ref('')
const orders = ref<any[]>([])
const itemsMap = ref<Record<number, any[]>>({})
const reviewedMap = ref<Record<number, boolean>>({})
const showLimit = 3
const expandedOrders = ref(new Set<number>())

function toggleExpand(orderId: number) {
  if (expandedOrders.value.has(orderId)) {
    expandedOrders.value.delete(orderId)
  } else {
    expandedOrders.value.add(orderId)
  }
}

const statusTypeMap: Record<string, string> = {
  '待接单': 'warning', '待配送': 'primary',
  '配送中': '', '已完成': 'success', '已取消': 'info',
}
function statusType(s: string) {
  return statusTypeMap[s] || 'info'
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

async function loadOrders() {
  loading.value = true
  const res: any = await consumerApi.getOrders(status.value || undefined)
  orders.value = res.orders || []
  itemsMap.value = res.itemsMap || {}
  reviewedMap.value = res.reviewedMap || {}
  loading.value = false
}

async function cancelOrder(id: number) {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    await consumerApi.cancelOrder(id)
    ElMessage.success('订单已取消')
    loadOrders()
  } catch { /* cancelled */ }
}

async function confirmReceived(id: number) {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', { type: 'success' })
    await consumerApi.confirmReceived(id)
    ElMessage.success('已确认收货')
    loadOrders()
  } catch { /* cancelled */ }
}

onMounted(loadOrders)
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #F5F5F5;
}

.orders-tabs {
  background: #FFF;
}

.orders-list {
  padding: 10px 0 0;
}

.oc-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.oc-shop {
  font-size: 15px;
  font-weight: 600;
}

.oc-time {
  font-size: 11px;
  color: #BBB;
  margin: 2px 0 6px;
}

.oc-item {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 13px;
  color: #666;
}

.oc-qty {
  color: #999;
}

.oc-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid #F0F0F0;
  font-size: 14px;
  margin-top: 6px;
}

.oc-bottom b {
  font-size: 15px;
}

.oc-actions {
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
