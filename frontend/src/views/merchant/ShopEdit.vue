<template>
  <div class="shop-edit-page">
    <div class="header-gradient" style="font-size:18px;">⚙️ 店铺设置</div>

    <div class="card" style="margin:10px 12px;">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="店铺名称"><el-input v-model="form.shopName" /></el-form-item>
        <el-form-item label="营业时间"><el-input v-model="form.businessHours" placeholder="如 09:00-22:00" /></el-form-item>
        <el-form-item label="配送费"><el-input v-model.number="form.deliveryFee" type="number" /></el-form-item>
        <el-form-item label="起送价"><el-input v-model.number="form.minOrderAmount" type="number" /></el-form-item>
        <el-form-item label="店铺头像"><el-input v-model="form.shopAvatar" placeholder="头像URL" /></el-form-item>
        <el-form-item label="店铺简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="handleSave" :loading="saving" style="width:100%">保存</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card" style="margin:10px 12px;">
      <div class="se-toggle-row">
        <span>营业状态：
          <strong :class="shop?.status === '营业中' ? 'se-open' : 'se-closed'">{{ shop?.status }}</strong>
        </span>
        <el-button
          :type="shop?.status === '营业中' ? 'danger' : 'success'"
          @click="handleToggle"
          :loading="toggling"
        >切换状态</el-button>
      </div>
    </div>

    <div class="card" style="margin:10px 12px;">
      <div style="font-weight:700;font-size:15px;margin-bottom:8px;">店铺信息</div>
      <div class="se-info-row"><span>评分</span><span>⭐{{ shop?.rating }}</span></div>
      <div class="se-info-row"><span>月销量</span><span>{{ shop?.monthlySales }}</span></div>
      <div class="se-info-row"><span>地址</span><span>{{ shop?.shopAddress }}</span></div>
      <div class="se-info-row"><span>营业执照</span><span>{{ shop?.businessLicense }}</span></div>
    </div>

    <!-- 退出登录 -->
    <div class="card" style="margin:20px 12px; text-align:center; cursor:pointer;" @click="handleLogout">
      <span style="color:#FF4D4F;font-size:15px;">退出登录</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api/merchant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const shop = ref<any>(null)
const saving = ref(false)
const toggling = ref(false)
const formRef = ref()
const form = reactive({
  shopName: '', shopAvatar: '', description: '',
  businessHours: '', deliveryFee: 0, minOrderAmount: 0,
})

async function load() {
  shop.value = await merchantApi.getShop()
  Object.assign(form, shop.value)
}
async function handleSave() {
  saving.value = true
  try { await merchantApi.updateShop(form); ElMessage.success('保存成功'); load() }
  finally { saving.value = false }
}
async function handleToggle() {
  toggling.value = true
  try { await merchantApi.toggleStatus(); ElMessage.success('状态已切换'); load() }
  finally { toggling.value = false }
}
function handleLogout() {
  store.logout()
  router.push('/login')
}
onMounted(load)
</script>

<style scoped>
.se-toggle-row { display: flex; justify-content: space-between; align-items: center; }
.se-open { color: #52C41A; }
.se-closed { color: #FF4D4F; }
.se-info-row { display: flex; justify-content: space-between; padding: 4px 0; font-size: 14px; color: #666; }
</style>
