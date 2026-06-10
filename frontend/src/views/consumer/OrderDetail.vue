<template>
  <div class="od-page">
    <div class="header-gradient" style="gap:12px;">
      <el-icon :size="20" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span style="font-size:18px;font-weight:700;">订单详情</span>
    </div>

    <!-- 步骤条 -->
    <div style="background:#FFF;padding:20px 16px;margin-bottom:10px;">
      <el-steps :active="order?.step" align-center>
        <el-step title="待接单" />
        <el-step title="待配送" />
        <el-step title="配送中" />
        <el-step title="已完成" />
      </el-steps>
    </div>

    <!-- 状态 -->
    <div style="text-align:center;padding:8px;" v-if="order">
      <el-tag :type="statusType(order.status)" size="large">{{ order.status }}</el-tag>
      <div v-if="order.status === '配送中'" style="color:#FFB800;font-weight:700;margin-top:4px;">
        预计 {{ order.estimatedDelivery?.substring(11, 16) }} 送达
      </div>
    </div>

    <!-- 商品 -->
    <div class="card" style="margin:0 12px 10px;">
      <div v-for="item in items" :key="item.id" class="odi-item">
        <span class="odi-name">{{ item.productName }}</span>
        <span class="odi-qty">x{{ item.quantity }}</span>
        <span class="odi-price">¥{{ item.subtotal }}</span>
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

    <!-- 收货信息 -->
    <div class="card" style="margin:0 12px 10px;">
      <div style="font-weight:700;font-size:15px;margin-bottom:6px;">收货信息</div>
      <p style="font-size:14px;color:#666;">{{ order?.addressSnapshot }}</p>
    </div>

    <!-- 订单信息 -->
    <div class="card" style="margin:0 12px 10px;">
      <div style="font-weight:700;font-size:15px;margin-bottom:6px;">订单信息</div>
      <div class="odi-info-row"><span>订单编号</span><span style="font-size:13px;">{{ order?.orderNo }}</span></div>
      <div class="odi-info-row"><span>下单时间</span><span>{{ order?.createdAt?.substring(0, 19) }}</span></div>
      <div class="odi-info-row"><span>支付时间</span><span>{{ order?.paidAt?.substring(0, 19) || '-' }}</span></div>
    </div>

    <!-- 操作按钮 -->
    <div style="display:flex;justify-content:center;gap:12px;padding:16px;" v-if="order">
      <el-button v-if="order.status === '待接单'" type="danger" @click="handleCancel">取消订单</el-button>
      <el-button v-if="order.status === '配送中'" type="success" @click="handleConfirm">确认收货</el-button>
      <el-button v-if="order.status === '已完成' && !reviewed" type="warning"
                 @click="$router.push(`/consumer/order/${order.id}/review`)">去评价</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'

const route = useRoute(); const router = useRouter()
const order = ref<any>(null); const items = ref<any[]>([]); const reviewed = ref(false)
const id = Number(route.params.id)

const itemsTotal = computed(() =>
  items.value.reduce((sum, it) => sum + (Number(it.subtotal) || 0), 0)
)
const couponDiscount = computed(() => {
  if (!order.value) return 0
  return Math.max(0, itemsTotal.value - Number(order.value.totalAmount))
})
function statusType(s: string) {
  const map: Record<string, string> = { '待接单': 'warning', '待配送': 'primary', '配送中': '', '已完成': 'success', '已取消': 'info' }
  return map[s] || 'info'
}
async function load() {
  const res: any = await consumerApi.getOrderDetail(id)
  order.value = res.order; items.value = res.items; reviewed.value = res.reviewed
}
async function handleCancel() {
  try { await ElMessageBox.confirm('确定取消？', '提示', { type: 'warning' }); await consumerApi.cancelOrder(id); ElMessage.success('已取消'); router.back() } catch { /* */ }
}
async function handleConfirm() {
  try { await ElMessageBox.confirm('确认收货？', '提示', { type: 'success' }); await consumerApi.confirmReceived(id); ElMessage.success('已确认收货'); load() } catch { /* */ }
}
onMounted(load)
</script>

<style scoped>
.odi-item { display: flex; align-items: center; gap: 8px; padding: 6px 0; font-size: 14px; }
.odi-name { flex: 1; }
.odi-qty { color: #999; }
.odi-price { color: #FF6B35; font-weight: 600; }
.odi-total { text-align: right; padding-top: 8px; border-top: 1px solid #F0F0F0; font-size: 14px; }
.odi-total span { font-size: 18px; color: #FF6B35; font-weight: 700; }
.odi-info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 14px; color: #666; }

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
