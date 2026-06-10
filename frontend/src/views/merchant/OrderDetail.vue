<template>
  <div class="mod-page">
    <div class="header-gradient" style="gap:12px;">
      <el-icon :size="20" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span style="font-size:18px;font-weight:700;">订单详情</span>
    </div>

    <div class="card" style="margin:10px 12px;">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <span style="font-size:14px;color:#999;">订单 {{ order?.orderNo }}</span>
        <el-tag :type="statusType(order?.status)">{{ order?.status }}</el-tag>
      </div>
    </div>

    <div class="card" style="margin:0 12px 10px;">
      <div style="font-weight:700;font-size:15px;margin-bottom:8px;">商品列表</div>
      <div v-for="item in items" :key="item.id" class="mod-item">
        <span>{{ item.productName }}</span>
        <span style="color:#999;">x{{ item.quantity }}</span>
        <span style="color:#FF6B35;font-weight:600;">¥{{ item.subtotal }}</span>
      </div>
      <div class="odi-summary">
        <div class="odi-sum-row"><span>商品小计</span><span>¥{{ itemsTotal.toFixed(2) }}</span></div>
        <div v-if="couponDiscount > 0" class="odi-sum-row" style="color:#FF6B35;">
          <span>🎫 优惠减免</span><span>−¥{{ couponDiscount.toFixed(2) }}</span>
        </div>
        <div class="odi-sum-row odi-sum-final">
          <span>实付</span><span>¥{{ order?.totalAmount }}</span>
        </div>
      </div>
    </div>

    <div class="card" style="margin:0 12px 10px;">
      <div style="font-weight:700;font-size:15px;margin-bottom:6px;">收货信息</div>
      <p style="font-size:14px;color:#666;">{{ order?.addressSnapshot }}</p>
    </div>

    <div style="display:flex;justify-content:center;gap:12px;padding:20px;" v-if="order">
      <el-button v-if="order.status === '待接单'" type="warning" size="large" @click="handleAction('accept')">接 单</el-button>
      <el-button v-if="order.status === '待配送'" type="primary" size="large" @click="handleAction('deliver')">开始配送</el-button>
      <el-button v-if="order.status === '配送中'" type="success" size="large" @click="handleAction('complete')">完成订单</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api/merchant'
const route = useRoute(); const router = useRouter()
const order = ref<any>(null); const items = ref<any[]>([])
const id = Number(route.params.id)

const itemsTotal = computed(() =>
  items.value.reduce((sum, it) => sum + (Number(it.subtotal) || 0), 0)
)
const couponDiscount = computed(() => {
  if (!order.value) return 0
  return Math.max(0, itemsTotal.value - Number(order.value.totalAmount))
})
function statusType(s: string) {
  const map: Record<string, string> = { '待接单': 'warning', '待配送': 'primary', '配送中': '', '已完成': 'success' }
  return map[s] || 'info'
}
async function load() { const res: any = await merchantApi.getOrderDetail(id); order.value = res.order; items.value = res.items }
async function handleAction(action: string) {
  if (action === 'accept') { await merchantApi.acceptOrder(id); ElMessage.success('已接单') }
  else if (action === 'deliver') { await merchantApi.deliverOrder(id); ElMessage.success('开始配送') }
  else if (action === 'complete') { await merchantApi.completeOrder(id); ElMessage.success('已完成') }
  router.back()
}
onMounted(load)
</script>

<style scoped>
.mod-item { display: flex; justify-content: space-between; padding: 4px 0; font-size: 14px; }
.mod-total { text-align: right; padding-top: 8px; border-top: 1px solid #F0F0F0; font-weight: 700; font-size: 16px; margin-top: 4px; }

.odi-summary {
  padding-top: 8px;
  border-top: 1px solid #F0F0F0;
  margin-top: 4px;
}

.odi-sum-row {
  display: flex;
  justify-content: space-between;
  padding: 3px 0;
  font-size: 14px;
  color: #666;
}

.odi-sum-final {
  font-size: 16px;
  font-weight: 700;
  color: #1A1A1A;
  padding-top: 6px;
  margin-top: 4px;
  border-top: 1px dashed #F0F0F0;
}

.odi-sum-final span:last-child {
  font-size: 20px;
  color: #FF6B35;
}
</style>
