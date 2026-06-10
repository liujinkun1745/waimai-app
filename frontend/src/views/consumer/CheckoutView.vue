<template>
  <div class="checkout-page">
    <!-- 头部 -->
    <div class="header-gradient" style="position:sticky;top:0;z-index:50;">
      <el-icon :size="22" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span>确认订单</span>
    </div>

    <!-- 地址选择 -->
    <div class="card co-addr" @click="showAddr = true">
      <span class="co-addr-icon">📍</span>
      <div class="co-addr-info" v-if="selectedAddr">
        <div class="co-addr-name">{{ selectedAddr.receiverName }} {{ selectedAddr.receiverPhone }}</div>
        <div class="co-addr-detail">{{ selectedAddr.province }}{{ selectedAddr.city }}{{ selectedAddr.district }} {{ selectedAddr.detailAddress }}</div>
      </div>
      <div class="co-addr-info" v-else>
        <div class="co-addr-name" style="color:#999;">请添加收货地址</div>
      </div>
      <span class="co-arrow">›</span>
    </div>

    <!-- 商家名称 -->
    <div style="padding:8px 14px;font-size:14px;font-weight:700;color:#FFB800;">
      {{ cart.merchantName }}
    </div>

    <!-- 商品清单 -->
    <div class="card" style="margin-top:0;">
      <div v-for="it in cart.items" :key="it.productId" class="co-item">
        <span class="co-item-name">{{ it.productName }}</span>
        <span class="co-item-qty">x{{ it.quantity }}</span>
        <span class="co-item-price">¥{{ (it.price * it.quantity).toFixed(2) }}</span>
      </div>
    </div>

    <!-- 优惠券 -->
    <div class="card co-row" @click="showCoupon = true">
      <span>🎫 优惠券</span>
      <span v-if="selectedCoupon" class="co-coupon-selected">
        −¥{{ selectedCoupon.amount }} <span class="co-arrow">›</span>
      </span>
      <span v-else style="color:#999;">
        {{ coupons.length }}张可用 <span class="co-arrow">›</span>
      </span>
    </div>

    <!-- 备注 -->
    <div class="card co-row" style="cursor:default;">
      <span>📝 备注</span>
      <el-input v-model="note" placeholder="选填，如口味要求等" size="small" style="width:180px;" />
    </div>

    <!-- 费用明细 -->
    <div class="card" style="margin-top:0;">
      <div class="co-fee-row"><span>商品小计</span><span>¥{{ cart.totalAmount.toFixed(2) }}</span></div>
      <div class="co-fee-row"><span>配送费</span><span>¥{{ deliveryFee.toFixed(2) }}</span></div>
      <div class="co-fee-row" v-if="selectedCoupon"><span>优惠券</span><span style="color:#FF6B35;">−¥{{ selectedCoupon.amount }}</span></div>
      <div class="co-fee-row co-fee-total">
        <span>实付</span>
        <span class="co-fee-price">¥{{ finalAmount.toFixed(2) }}</span>
      </div>
    </div>

    <!-- 底部提交栏 -->
    <div class="bottom-bar" style="z-index:100;">
      <div class="co-bar-left">
        合计 <b class="co-bar-total">¥{{ finalAmount.toFixed(2) }}</b>
      </div>
      <div class="co-bar-right" :class="{ loading }" @click="submitOrder">
        {{ loading ? '提交中...' : '提交订单' }}
      </div>
    </div>

    <!-- 地址选择弹窗 -->
    <el-dialog v-model="showAddr" title="选择地址" width="88%">
      <div v-if="!addresses.length" style="text-align:center;padding:20px;color:#999;">
        暂无地址，请添加
      </div>
      <div
        v-for="a in addresses"
        :key="a.id"
        class="addr-option"
        :class="{ on: a.id === selectedAddr?.id }"
        @click="selectAddress(a)"
      >
        <div class="addr-option-top">
          {{ a.receiverName }} {{ a.receiverPhone }}
          <el-tag v-if="a.isDefault" size="small" type="warning">默认</el-tag>
        </div>
        <div class="addr-option-bot">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detailAddress }}</div>
      </div>
      <el-divider />
      <el-button type="warning" @click="showAddrAdd = true" plain style="width:100%;">+ 新增地址</el-button>
    </el-dialog>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="showAddrAdd" title="新增地址" width="88%">
      <el-form :model="addrForm" size="large">
        <el-form-item label="收货人">
          <el-input v-model="addrForm.receiverName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="addrForm.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="addrForm.detailAddress" placeholder="省市区+详细地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="addAddress" :loading="adding" style="width:100%;">保存</el-button>
        </el-form-item>
      </el-form>
    </el-dialog>

    <!-- 优惠券弹窗 -->
    <el-dialog v-model="showCoupon" title="选择优惠券" width="88%">
      <div class="coupon-option" @click="selectedCoupon = null; showCoupon = false;">
        <span>不使用优惠券</span>
        <el-icon v-if="!selectedCoupon"><Select /></el-icon>
      </div>
      <div
        v-for="c in coupons"
        :key="c.id"
        class="coupon-option"
        @click="selectedCoupon = c; showCoupon = false;"
      >
        <div>
          <div class="coupon-name">{{ c.name }}</div>
          <div class="coupon-cond">{{ c.minOrder > 0 ? `满¥${c.minOrder}可用` : '无门槛' }}</div>
        </div>
        <div class="coupon-amount">
          −¥{{ c.amount }}
          <el-icon v-if="selectedCoupon?.id === c.id"><Select /></el-icon>
        </div>
      </div>
      <el-empty v-if="!coupons.length" description="暂无可用优惠券" :image-size="50" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Select } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const merchantId = Number(route.params.merchantId)

const addresses = ref<any[]>([])
const selectedAddr = ref<any>(null)
const coupons = ref<any[]>([])
const selectedCoupon = ref<any>(null)
const deliveryFee = ref(3)
const note = ref('')
const loading = ref(false)
const adding = ref(false)
const showAddr = ref(false)
const showAddrAdd = ref(false)
const showCoupon = ref(false)

const addrForm = reactive({ receiverName: '', receiverPhone: '', detailAddress: '' })

const finalAmount = computed(() => {
  let amount = cart.totalAmount + deliveryFee.value
  if (selectedCoupon.value) amount -= Number(selectedCoupon.value.amount)
  return Math.max(0, amount)
})

async function loadData() {
  const [addrRes, cpnRes, shopRes] = await Promise.all([
    consumerApi.getAddresses(),
    consumerApi.getCoupons(),
    consumerApi.getMerchantDetail(merchantId),
  ])
  addresses.value = addrRes || []
  selectedAddr.value = (addrRes || []).find((a: any) => a.isDefault) || (addrRes || [])[0] || null
  coupons.value = cpnRes || []
  const shop = shopRes?.merchant
  if (shop) {
    deliveryFee.value = shop.deliveryFee || 3
  }
}

function selectAddress(a: any) {
  selectedAddr.value = a
  showAddr.value = false
}

async function addAddress() {
  if (!addrForm.receiverName || !addrForm.receiverPhone || !addrForm.detailAddress) {
    ElMessage.warning('请填写完整信息')
    return
  }
  adding.value = true
  try {
    await consumerApi.addAddress({ ...addrForm, province: '', city: '', district: '', isDefault: false })
    ElMessage.success('地址已添加')
    showAddrAdd.value = false
    const res = await consumerApi.getAddresses()
    addresses.value = res || []
    if (!selectedAddr.value) selectedAddr.value = (res || [])[0]
  } finally {
    adding.value = false
  }
}

async function submitOrder() {
  if (!selectedAddr.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  loading.value = true
  try {
    const items = cart.items.map(i => ({ productId: i.productId, quantity: i.quantity }))
    await consumerApi.submitOrder({
      merchantId,
      addressId: selectedAddr.value.id,
      items,
      totalAmount: finalAmount.value,
      couponId: selectedCoupon.value?.id || 0,
    })
    cart.clear()
    ElMessage.success('下单成功！🎉')
    router.push('/consumer/orders')
  } catch {
    /* handled by interceptor */
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.checkout-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 64px;
}

.co-addr {
  cursor: pointer;
  display: flex;
  gap: 10px;
  align-items: center;
}

.co-addr-icon { font-size: 20px; }

.co-addr-info { flex: 1; }

.co-addr-name { font-size: 15px; font-weight: 600; }

.co-addr-detail { font-size: 12px; color: #999; margin-top: 2px; }

.co-arrow { font-size: 22px; color: #CCC; }

.co-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 14px;
}

.co-item-name { flex: 1; }
.co-item-qty { color: #999; }
.co-item-price { color: #FF6B35; font-weight: 600; }

.co-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.co-coupon-selected { color: #FF6B35; font-weight: 600; }

.co-fee-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
  color: #666;
}

.co-fee-total {
  font-size: 16px;
  font-weight: 700;
  color: #1A1A1A;
  padding-top: 8px;
  border-top: 1px solid #F0F0F0;
  margin-top: 4px;
}

.co-fee-price { font-size: 22px; color: #FF6B35; }

/* 底部栏 */
.co-bar-left {
  flex: 1;
  padding-left: 14px;
  font-size: 14px;
}

.co-bar-total { font-size: 22px; color: #FF6B35; margin-left: 4px; }

.co-bar-right {
  height: 100%;
  padding: 0 28px;
  display: flex;
  align-items: center;
  background: #FFB800;
  color: #FFF;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}

.co-bar-right:active { background: #FFA000; }
.co-bar-right.loading { background: #CCC; cursor: default; }

/* 地址选项 */
.addr-option {
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  border: 1px solid #EEE;
  cursor: pointer;
}

.addr-option.on {
  border-color: #FFB800;
  background: #FFF8E1;
}

.addr-option-top {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.addr-option-bot { font-size: 12px; color: #999; margin-top: 4px; }

/* 优惠券选项 */
.coupon-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #F0F0F0;
  cursor: pointer;
}

.coupon-name { font-size: 15px; font-weight: 600; }
.coupon-cond { font-size: 12px; color: #999; }
.coupon-amount {
  font-size: 17px;
  font-weight: 700;
  color: #FF6B35;
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>
