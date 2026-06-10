<template>
  <div class="md-page">
    <!-- 飞入动画小球 -->
    <span
      v-for="f in flyItems"
      :key="f.id"
      class="fly-wrapper"
      :style="{ '--sx': f.x + 'px', '--sy': f.y + 'px', '--dx': f.dx + 'px', '--dy': f.dy + 'px' }"
    >
      <span class="fly-dot">＋</span>
    </span>

    <!-- 顶部商家信息 + 标签切换 -->
    <div class="header-gradient" style="gap: 10px; flex-wrap: wrap; padding-bottom: 10px;">
      <div style="display:flex;align-items:center;gap:10px;width:100%;">
        <el-icon :size="22" @click="$router.back()" style="cursor:pointer;">
          <ArrowLeft />
        </el-icon>
        <span style="flex:1;font-weight:700;">{{ merchant?.shopName }}</span>
        <span style="font-size:13px;font-weight:400;">⭐{{ merchant?.rating }}</span>
      </div>
      <div class="md-tabs">
        <span class="md-tab" :class="{ on: activeTab === 'menu' }" @click="activeTab = 'menu'">点菜</span>
        <span class="md-tab" :class="{ on: activeTab === 'review' }" @click="activeTab = 'review'">评价 {{ reviews.length }}</span>
      </div>
    </div>

    <!-- 主体：左侧分类 + 右侧商品列表 -->
    <div class="md-body">
      <!-- 左侧分类栏 -->
      <div class="md-sidebar">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="md-sidebar-item"
          :class="{ on: activeCat === cat.id }"
          @click="switchCategory(cat.id)"
        >
          {{ cat.name }}
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="md-content" ref="contentRef">
        <!-- 菜单模式 -->
        <template v-if="activeTab === 'menu'">
          <div v-if="!products.length" class="md-empty">
            <el-empty description="暂无商品" :image-size="50" />
          </div>
          <div
            v-for="p in products"
            :key="p.id"
            class="md-product"
          >
            <div class="mdp-avatar">🍽️</div>
            <div class="mdp-info">
              <div class="mdp-name">{{ p.name }}</div>
              <div class="mdp-desc" v-if="p.description">{{ p.description }}</div>
              <div class="mdp-meta">
                已售{{ p.sales }}
                <span v-if="p.stock <= 0" class="mdp-soldout">售罄</span>
              </div>
              <div class="mdp-bottom">
                <span class="mdp-price">¥{{ p.price }}</span>
                <div class="mdp-quantity">
                  <span
                    v-if="cartQty(p.id)"
                    class="mdp-btn-minus"
                    @click.stop="cart.updateQuantity(p.id, cartQty(p.id) - 1)"
                  >─</span>
                  <span v-if="cartQty(p.id)" class="mdp-qty-num" :key="'q'+p.id+cartQty(p.id)">{{ cartQty(p.id) }}</span>
                  <span
                    class="mdp-btn-plus"
                    @click.stop="addToCart(p, $event)"
                  >＋</span>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- 评价模式 -->
        <template v-else>
          <div class="md-review-header">
            <div class="mdr-score">{{ merchant?.rating }}</div>
            <div class="mdr-stars">⭐⭐⭐⭐⭐</div>
            <div class="mdr-total">{{ reviews.length }} 条评价</div>
          </div>

          <div class="md-review-dist">
            <div v-for="i in 5" :key="i" class="mdr-bar-row">
              <span class="mdr-bar-label">{{ 6-i }}星</span>
              <span class="mdr-bar-track">
                <span class="mdr-bar-fill" :style="{ width: distPercent(6-i) + '%' }"></span>
              </span>
              <span class="mdr-bar-count">{{ dist[6-i] || 0 }}</span>
            </div>
          </div>

          <div v-if="!reviews.length" class="md-empty">
            <el-empty description="暂无评价" :image-size="50" />
          </div>

          <div v-for="r in reviews" :key="r.id" class="md-review-item">
            <div class="mdri-user">
              <span class="mdri-avatar">{{ r.consumer?.username?.charAt(0) || 'U' }}</span>
              <div>
                <div class="mdri-username">{{ r.consumer?.username || '匿名用户' }}</div>
                <div class="mdri-date">{{ formatDate(r.createdAt) }}</div>
              </div>
            </div>
            <div class="mdri-ratings">
              <span class="mdri-tag">😋 口味 {{ r.tasteRating }}</span>
              <span class="mdri-tag">📦 包装 {{ r.packagingRating }}</span>
              <span class="mdri-tag">🚀 配送 {{ r.deliveryRating }}</span>
            </div>
            <div class="mdri-comment" v-if="r.comment">{{ r.comment }}</div>
            <div class="mdri-reply" v-if="r.reply">
              <span class="mdri-reply-tag">商家回复</span>{{ r.reply }}
            </div>
          </div>
        </template>
      </div>
    </div>

    <!-- 底部购物车栏 -->
    <div class="bottom-bar md-cart-bar" @click="cart.totalCount > 0 ? showDrawer = true : null" style="height:50px;">
      <div class="mdc-left">
        <span class="mdc-cart-icon" :key="'cart-bounce-'+cartBounce">🛒</span>
        <span v-if="cart.totalCount > 0" class="mdc-cart-total">¥{{ cart.totalAmount.toFixed(2) }}</span>
        <span v-else class="mdc-cart-empty">购物车空空如也</span>
      </div>
      <div
        class="mdc-right"
        :class="{ active: cart.totalCount > 0 && !belowMin }"
        @click.stop="goCheckout"
      >
        {{ cart.totalCount === 0 ? '去选购' : belowMin ? `差¥${gap}起送` : '去结算' }}
      </div>
    </div>

    <!-- 购物车抽屉 -->
    <el-drawer
      v-model="showDrawer"
      direction="btt"
      size="auto"
      :with-header="false"
      class="cart-drawer"
    >
      <div class="cdr">
        <div class="cdr-header">
          <span class="cdr-title">🛒 购物车</span>
          <span class="cdr-clear" @click="cart.clear()">🗑 清空</span>
        </div>

        <div v-if="!cart.items.length" class="cdr-empty">
          <span class="cdr-empty-icon">🛒</span>
          <span>购物车是空的</span>
          <span class="cdr-empty-sub">快去选几道美味吧～</span>
        </div>

        <div class="cdr-list">
          <div v-for="it in cart.items" :key="it.productId" class="cdr-item">
            <div class="cdr-item-avatar">🍽️</div>
            <div class="cdr-item-body">
              <div class="cdr-item-name">{{ it.productName }}</div>
              <div class="cdr-item-price">¥{{ it.price }}</div>
            </div>
            <div class="cdr-item-ctrl">
              <span class="cdr-ctrl-btn cdr-ctrl-minus" @click="cart.updateQuantity(it.productId, it.quantity - 1)">−</span>
              <span class="cdr-ctrl-num">{{ it.quantity }}</span>
              <span class="cdr-ctrl-btn cdr-ctrl-plus" @click="cart.updateQuantity(it.productId, it.quantity + 1)">+</span>
            </div>
          </div>
        </div>

        <div class="cdr-footer">
          <div class="cdr-footer-total">
            <span>合计</span>
            <span class="cdr-footer-price">¥{{ cart.totalAmount.toFixed(2) }}</span>
          </div>
          <div
            class="cdr-footer-btn"
            :class="{ active: !belowMin }"
            @click="goCheckout"
          >
            {{ belowMin ? `还差 ¥${gap} 起送` : '去结算' }}
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const id = Number(route.params.id)

const merchant = ref<any>(null)
const categories = ref<any[]>([])
const products = ref<any[]>([])
const reviews = ref<any[]>([])
const dist = ref<number[]>([0, 0, 0, 0, 0, 0])
const activeCat = ref(0)
const activeTab = ref<'menu' | 'review'>('menu')
const showDrawer = ref(false)
const contentRef = ref<HTMLElement>()

// 飞入动画
interface FlyItem { id: number; x: number; y: number; dx: number; dy: number }
const flyItems = ref<FlyItem[]>([])
const cartBounce = ref(0)
let flyId = 0

function triggerFly(event: MouseEvent) {
  const btn = event.currentTarget as HTMLElement
  const btnRect = btn.getBoundingClientRect()
  const sx = btnRect.left + btnRect.width / 2
  const sy = btnRect.top + btnRect.height / 2

  // 获取购物车图标位置作为终点
  const cartIcon = document.querySelector('.mdc-cart-icon')
  let tx = sx - 40   // fallback
  let ty = window.innerHeight - 30
  if (cartIcon) {
    const cr = cartIcon.getBoundingClientRect()
    tx = cr.left + cr.width / 2
    ty = cr.top + cr.height / 2
  }

  const item: FlyItem = {
    id: ++flyId,
    x: sx, y: sy,
    dx: tx - sx,
    dy: ty - sy,
  }
  flyItems.value.push(item)
  setTimeout(() => {
    flyItems.value = flyItems.value.filter(f => f.id !== item.id)
    cartBounce.value++
  }, 500)
}

const belowMin = computed(() =>
  merchant.value ? cart.totalAmount < merchant.value.minOrderAmount : false
)
const gap = computed(() =>
  merchant.value ? (merchant.value.minOrderAmount - cart.totalAmount).toFixed(2) : '0'
)

function cartQty(pid: number) {
  return cart.items.find(i => i.productId === pid)?.quantity || 0
}

function distPercent(star: number) {
  if (!reviews.value.length) return 0
  return Math.round(((dist.value[star] || 0) / reviews.value.length) * 100)
}

function formatDate(d: string) {
  return d ? d.substring(0, 10) : ''
}

async function loadData() {
  const res: any = await consumerApi.getMerchantDetail(id)
  merchant.value = res.merchant
  categories.value = res.categories || []
  products.value = res.products || []
  reviews.value = res.reviews || []

  const d = [0, 0, 0, 0, 0, 0]
  ;(res.reviews || []).forEach((rv: any) => {
    const s = Math.round((rv.tasteRating + rv.packagingRating + rv.deliveryRating) / 3)
    if (s >= 1 && s <= 5) d[s]++
  })
  dist.value = d

  if (res.categories?.length) activeCat.value = res.categories[0].id

  cart.setMerchant(id, res.merchant?.shopName || '')
}

async function switchCategory(catId: number) {
  activeCat.value = catId
  activeTab.value = 'menu'
  products.value = await consumerApi.getProductsByCategory(id, catId)
}

function addToCart(p: any, event: MouseEvent) {
  if (p.stock <= 0) {
    ElMessage.warning('该商品已售罄')
    return
  }
  cart.addItem(p, 1)
  triggerFly(event)
}

function goCheckout() {
  if (cart.totalCount === 0 || belowMin.value) return
  showDrawer.value = false
  router.push(`/consumer/checkout/${id}`)
}

onMounted(loadData)
</script>

<style scoped>
/* 头部标签切换 */
.md-tabs {
  display: flex;
  gap: 0;
  width: 100%;
  margin-top: 8px;
  background: rgba(255,255,255,0.3);
  border-radius: 20px;
  overflow: hidden;
}

.md-tab {
  flex: 1;
  text-align: center;
  padding: 8px 0;
  font-size: 14px;
  font-weight: 500;
  color: #886600;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 20px;
}

.md-tab.on {
  background: #FFF;
  color: #1A1A1A;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

/* 主体 */
.md-body {
  display: flex;
  flex: 1;
  height: calc(100vh - 134px); /* header 84 + bottom-bar 50 */
  overflow: hidden;
}

/* 左侧分类栏 */
.md-sidebar {
  width: 82px;
  background: #F8F8F8;
  overflow-y: auto;
  flex-shrink: 0;
  padding: 4px 0;
}

.md-sidebar-item {
  padding: 14px 6px;
  font-size: 13px;
  color: #666;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.md-sidebar-item.on {
  background: #FFF;
  color: #1A1A1A;
  font-weight: 700;
}

.md-sidebar-item.on::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  bottom: 6px;
  width: 3px;
  background: #FFB800;
  border-radius: 0 2px 2px 0;
}

.md-sidebar-spacer {
  height: 1px;
  background: #E8E8E8;
  margin: 6px 10px;
}

/* 右侧内容 */
.md-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 8px;
}

.md-empty {
  padding-top: 60px;
}

/* 商品卡片 */
.md-product {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid #F0F0F0;
  align-items: flex-start;
}

.mdp-avatar {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background: #FFF8E1;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.mdp-info {
  flex: 1;
  min-width: 0;
}

.mdp-name {
  font-size: 15px;
  font-weight: 600;
}

.mdp-desc {
  font-size: 12px;
  color: #AAA;
  margin: 2px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mdp-meta {
  font-size: 11px;
  color: #BBB;
}

.mdp-soldout {
  color: #FF4D4F;
  font-weight: 600;
  margin-left: 6px;
}

.mdp-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}

.mdp-price {
  font-size: 17px;
  font-weight: 700;
  color: #FF6B35;
}

.mdp-quantity {
  display: flex;
  align-items: center;
  gap: 2px;
}

.mdp-btn-plus {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #FFB800;
  color: #FFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.mdp-btn-minus {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid #FFB800;
  color: #FFB800;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
}

.mdp-qty-num {
  width: 22px;
  text-align: center;
  font-size: 14px;
  font-weight: 600;
  animation: bounceIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes bounceIn {
  0%   { transform: scale(0.3); opacity: 0; }
  50%  { transform: scale(1.2); }
  100% { transform: scale(1); opacity: 1; }
}

/* 评价区 */
.md-review-header {
  text-align: center;
  padding: 20px 0 12px;
}

.mdr-score {
  font-size: 40px;
  font-weight: 800;
  color: #FF6B35;
}

.mdr-stars {
  font-size: 16px;
  margin: 4px 0;
}

.mdr-total {
  font-size: 12px;
  color: #999;
}

.md-review-dist {
  padding: 0 4px 12px;
}

.mdr-bar-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 3px 0;
}

.mdr-bar-label {
  width: 28px;
  font-size: 12px;
  color: #999;
  text-align: right;
}

.mdr-bar-track {
  flex: 1;
  height: 6px;
  background: #F0F0F0;
  border-radius: 3px;
  overflow: hidden;
}

.mdr-bar-fill {
  display: block;
  height: 100%;
  background: #FFB800;
  border-radius: 3px;
}

.mdr-bar-count {
  width: 20px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.md-review-item {
  padding: 14px 0;
  border-bottom: 1px solid #F0F0F0;
}

.mdri-user {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.mdri-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #FFF8E1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #FFB800;
}

.mdri-username {
  font-size: 14px;
  font-weight: 600;
}

.mdri-date {
  font-size: 11px;
  color: #BBB;
}

.mdri-ratings {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.mdri-tag {
  font-size: 11px;
  color: #999;
  background: #F5F5F5;
  padding: 2px 8px;
  border-radius: 10px;
}

.mdri-comment {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 4px;
}

.mdri-reply {
  margin-top: 6px;
  padding: 8px 10px;
  background: #FFF8E1;
  border-radius: 8px;
  font-size: 13px;
  color: #666;
}

.mdri-reply-tag {
  display: inline-block;
  background: #FFB800;
  color: #FFF;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  margin-right: 6px;
}

/* 底部购物车栏 */
.md-cart-bar {
  background: #3D3D3D;
  cursor: pointer;
}

.mdc-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 14px;
}

.mdc-cart-icon {
  font-size: 24px;
}

.mdc-cart-total {
  font-size: 18px;
  font-weight: 700;
  color: #FFF;
}

.mdc-cart-empty {
  font-size: 14px;
  color: #999;
}

.mdc-right {
  height: 100%;
  padding: 0 22px;
  display: flex;
  align-items: center;
  background: #555;
  font-size: 15px;
  font-weight: 700;
  color: #999;
  cursor: default;
  transition: all 0.2s;
}

.mdc-right.active {
  background: #FFB800;
  color: #FFF;
  cursor: pointer;
}

/* 购物车抽屉 */
.cdr {
  display: flex;
  flex-direction: column;
  max-height: 55vh;
  padding: 16px;
}

.cdr-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #F0F0F0;
}

.cdr-title {
  font-size: 18px;
  font-weight: 700;
}

.cdr-clear {
  font-size: 14px;
  color: #999;
  cursor: pointer;
}

.cdr-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 30px 0;
  color: #CCC;
}

.cdr-empty-icon {
  font-size: 48px;
}

.cdr-empty-sub {
  font-size: 13px;
  color: #DDD;
}

.cdr-list {
  overflow-y: auto;
  padding: 8px 0;
  max-height: 35vh;
}

.cdr-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #F8F8F8;
}

.cdr-item-avatar {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: #FFF8E1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.cdr-item-body {
  flex: 1;
}

.cdr-item-name {
  font-size: 15px;
  font-weight: 500;
}

.cdr-item-price {
  font-size: 14px;
  color: #FF6B35;
  font-weight: 600;
  margin-top: 2px;
}

.cdr-item-ctrl {
  display: flex;
  align-items: center;
  gap: 0;
}

.cdr-ctrl-btn {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  font-weight: 700;
}

.cdr-ctrl-minus {
  border: 1.5px solid #DDD;
  color: #999;
}

.cdr-ctrl-plus {
  background: #FFB800;
  color: #FFF;
}

.cdr-ctrl-num {
  width: 28px;
  text-align: center;
  font-size: 15px;
  font-weight: 600;
}

.cdr-footer {
  padding-top: 12px;
  border-top: 1px solid #F0F0F0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.cdr-footer-total {
  flex: 1;
}

.cdr-footer-total span:first-child {
  font-size: 14px;
  color: #666;
}

.cdr-footer-price {
  font-size: 22px;
  font-weight: 800;
  color: #FF6B35;
  margin-left: 4px;
}

.cdr-footer-btn {
  height: 44px;
  padding: 0 32px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #CCC;
  color: #999;
  font-size: 16px;
  font-weight: 700;
  cursor: default;
  transition: all 0.2s;
}

.cdr-footer-btn.active {
  background: linear-gradient(135deg, #FFB800, #FF8F00);
  color: #FFF;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(255, 184, 0, 0.4);
}

/* 飞入动画 */
.fly-wrapper {
  position: fixed;
  z-index: 300;
  pointer-events: none;
  left: var(--sx);
  top: var(--sy);
  animation: flyX 0.5s linear forwards;
}

.fly-dot {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: #FFB800;
  border-radius: 50%;
  color: #FFF;
  font-size: 15px;
  font-weight: 800;
  box-shadow: 0 2px 8px rgba(255, 150, 0, 0.4);
  animation: flyY 0.5s ease-in forwards;
}

@keyframes flyX {
  to { transform: translateX(var(--dx)); }
}

@keyframes flyY {
  0%   { transform: translateY(0) scale(1); opacity: 1; }
  100% { transform: translateY(var(--dy)) scale(0.3); opacity: 0.3; }
}

/* 购物车弹跳 */
.mdc-cart-icon {
  animation: cartPop 0.3s ease;
}
@keyframes cartPop {
  0%   { transform: scale(1); }
  50%  { transform: scale(1.45); }
  100% { transform: scale(1); }
}
</style>
