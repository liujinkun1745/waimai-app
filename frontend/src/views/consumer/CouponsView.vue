<template>
  <div class="coupons-page">
    <!-- 头部 -->
    <div class="coupons-header">
      <span class="coupons-title">🎁 神券中心</span>
      <span class="coupons-subtitle">每天都有新惊喜</span>
    </div>

    <!-- 每日神券三选一 -->
    <div class="coupons-section">
      <div class="cs-title-row">
        <span class="cs-title">🔥 每日神券 · 三选一</span>
        <span class="cs-tip">今日有效，先到先得</span>
      </div>

      <div class="cs-daily-list">
        <div
          v-for="c in daily"
          :key="c.name"
          class="cs-red-envelope"
          :class="{ claimed: c.claimed }"
          @click="!c.claimed && claimCoupon(c)"
        >
          <div class="csre-top">
            <span class="csre-amount"><b>¥</b>{{ c.amount }}</span>
            <span class="csre-tag" v-if="c.claimed">已领</span>
            <span class="csre-tag csre-tag-go" v-else>领取</span>
          </div>
          <div class="csre-name">{{ c.name }}</div>
          <div class="csre-cond">{{ c.minOrder > 0 ? `满 ¥${c.minOrder} 可用` : '无门槛' }}</div>
        </div>
      </div>
    </div>

    <!-- 已有优惠券 -->
    <div class="coupons-section">
      <div class="cs-title-row">
        <span class="cs-title">🎫 我的优惠券</span>
        <span class="cs-tip">{{ myCoupons.length }} 张可用</span>
      </div>

      <div v-if="!myCoupons.length" class="cs-empty">暂无优惠券，快去领神券吧～</div>

      <div v-for="c in myCoupons" :key="c.id" class="cs-card">
        <div class="csc-left">
          <div class="csc-amount"><b>¥</b>{{ c.amount }}</div>
          <div class="csc-cond">{{ c.minOrder > 0 ? `满${c.minOrder}可用` : '无门槛' }}</div>
        </div>
        <div class="csc-middle">
          <div class="csc-name">{{ c.name }}</div>
          <div class="csc-info">下单时自动抵扣</div>
        </div>
        <div class="csc-right">
          <span class="csc-use" @click="$router.push('/consumer/home')">去使用 ›</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'

const daily = ref<any[]>([])
const myCoupons = ref<any[]>([])

async function loadCoupons() {
  const [d, m] = await Promise.all([
    consumerApi.getDailyCoupons(),
    consumerApi.getCoupons(),
  ])
  daily.value = d?.coupons || []
  myCoupons.value = m || []
}

async function claimCoupon(c: any) {
  const res: any = await consumerApi.claimCoupon({
    name: c.name, amount: c.amount, minOrder: c.minOrder,
  })
  if (res?.ok !== false) {
    ElMessage.success('领取成功！🎉')
    loadCoupons()
  }
}

onMounted(loadCoupons)
</script>

<style scoped>
.coupons-header {
  background: linear-gradient(180deg, #FFD101, #FFB800);
  padding: 20px 16px 24px;
  text-align: center;
}

.coupons-title {
  display: block;
  font-size: 22px;
  font-weight: 800;
}

.coupons-subtitle {
  display: block;
  font-size: 13px;
  margin-top: 4px;
  opacity: 0.8;
}

/* 区块 */
.coupons-section {
  padding: 14px;
}

.cs-title-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.cs-title {
  font-size: 16px;
  font-weight: 700;
}

.cs-tip {
  font-size: 12px;
  color: #999;
}

.cs-empty {
  text-align: center;
  padding: 30px 0;
  color: #CCC;
  font-size: 14px;
}

/* 三选一红包 */
.cs-daily-list {
  display: flex;
  gap: 10px;
}

.cs-red-envelope {
  flex: 1;
  background: linear-gradient(180deg, #FF4D4F 0%, #FF7875 100%);
  border-radius: 12px;
  padding: 18px 10px 14px;
  text-align: center;
  color: #FFF;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform 0.15s;
}

.cs-red-envelope::before {
  content: '';
  position: absolute;
  top: -10px;
  left: -10px;
  width: 20px;
  height: 20px;
  background: #F5F5F5;
  border-radius: 50%;
}

.cs-red-envelope::after {
  content: '';
  position: absolute;
  top: -10px;
  right: -10px;
  width: 20px;
  height: 20px;
  background: #F5F5F5;
  border-radius: 50%;
}

.cs-red-envelope:active {
  transform: scale(0.96);
}

.cs-red-envelope.claimed {
  background: linear-gradient(180deg, #CCC, #DDD);
  cursor: default;
}

.csre-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.csre-amount {
  font-size: 28px;
  font-weight: 800;
}

.csre-amount b {
  font-size: 16px;
  font-weight: 600;
}

.csre-tag {
  font-size: 10px;
  background: rgba(255, 255, 255, 0.3);
  padding: 2px 8px;
  border-radius: 8px;
}

.csre-tag-go {
  background: #FFD101;
  color: #1A1A1A;
  font-weight: 700;
}

.csre-name {
  font-size: 13px;
  margin: 10px 0 4px;
  opacity: 0.9;
}

.csre-cond {
  font-size: 11px;
  opacity: 0.7;
}

/* 已有券卡片 */
.cs-card {
  display: flex;
  background: #FFF;
  border-radius: 12px;
  margin-bottom: 10px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.csc-left {
  width: 85px;
  text-align: center;
  padding: 16px 6px;
  background: linear-gradient(180deg, #FFF8E1, #FFF3CD);
  flex-shrink: 0;
}

.csc-amount {
  font-size: 24px;
  font-weight: 800;
  color: #FF6B35;
}

.csc-amount b {
  font-size: 14px;
}

.csc-cond {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.csc-middle {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 12px;
}

.csc-name {
  font-size: 15px;
  font-weight: 600;
}

.csc-info {
  font-size: 12px;
  color: #BBB;
  margin-top: 4px;
}

.csc-right {
  display: flex;
  align-items: center;
  padding: 0 14px;
}

.csc-use {
  font-size: 13px;
  color: #FFB800;
  white-space: nowrap;
  cursor: pointer;
}
</style>
