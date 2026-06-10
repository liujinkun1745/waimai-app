<template>
  <div class="profile-page">
    <!-- 个人头部 -->
    <div class="profile-header">
      <div class="ph-avatar">{{ user?.username?.charAt(0)?.toUpperCase() || 'U' }}</div>
      <div class="ph-info">
        <div class="ph-name">{{ user?.username || '用户' }}</div>
        <div class="ph-phone">{{ user?.phone || '' }}</div>
      </div>
    </div>

    <!-- 资产卡片 -->
    <div class="profile-assets">
      <div class="pa-item" @click="$router.push('/consumer/balance')">
        <span class="pa-label">💰 余额</span>
        <span class="pa-value">¥{{ user?.balance || '0.00' }}</span>
      </div>
      <div class="pa-item" @click="$router.push('/consumer/coupons')">
        <span class="pa-label">🎫 优惠券</span>
        <span class="pa-value">查看 ›</span>
      </div>
    </div>

    <!-- 功能菜单 -->
    <div class="profile-menu card" style="margin: 12px 14px;">
      <div class="pm-item" @click="$router.push('/consumer/address')">
        <span>📍 收货地址</span>
        <span class="pm-arrow">›</span>
      </div>
      <div class="pm-item" @click="$router.push('/consumer/orders')">
        <span>📋 我的订单</span>
        <span class="pm-arrow">›</span>
      </div>
      <div class="pm-item" @click="showPwd = true" style="border:none;">
        <span>🔒 修改密码</span>
        <span class="pm-arrow">›</span>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="profile-logout card" style="margin: 20px 14px; text-align:center;" @click="handleLogout">
      <span style="color:#FF4D4F;font-size:15px;cursor:pointer;">退出登录</span>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPwd" title="修改密码" width="85%">
      <el-form ref="formRef" :model="pwdForm" :rules="pwdRules" size="large">
        <el-form-item prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="原密码" show-password />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="新密码（6-50位）" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPwd = false">取消</el-button>
        <el-button type="warning" @click="changePassword" :loading="pwdLoading">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()

const user = ref<any>(null)
const showPwd = ref(false)
const pwdLoading = ref(false)
const formRef = ref()

const pwdForm = reactive({ oldPassword: '', newPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 6, max: 50, message: '新密码6-50位', trigger: 'blur' }],
}

async function loadProfile() {
  user.value = await consumerApi.getProfile()
}

async function changePassword() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  pwdLoading.value = true
  try {
    await consumerApi.changePassword(pwdForm)
    ElMessage.success('密码修改成功')
    showPwd.value = false
  } finally {
    pwdLoading.value = false
  }
}

function handleLogout() {
  store.logout()
  router.push('/login')
}

onMounted(loadProfile)
</script>

<style scoped>
/* 头部 */
.profile-header {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 24px 20px 30px;
  background: linear-gradient(180deg, #FFD101 0%, #FFD101 40%, #FFF8E1 100%);
}

.ph-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #FFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: #FFB800;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ph-name {
  font-size: 18px;
  font-weight: 700;
}

.ph-phone {
  font-size: 14px;
  color: #666;
  margin-top: 2px;
}

/* 资产 */
.profile-assets {
  display: flex;
  margin: -12px 14px 0;
  gap: 10px;
  position: relative;
  z-index: 1;
}

.pa-item {
  flex: 1;
  padding: 16px;
  background: #FFF;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
}

.pa-label {
  display: block;
  font-size: 12px;
  color: #999;
}

.pa-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: #FF6B35;
  margin-top: 6px;
}

/* 菜单 */
.pm-item {
  padding: 14px 0;
  border-bottom: 1px solid #F5F5F5;
  font-size: 15px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pm-arrow {
  color: #CCC;
  font-size: 18px;
}

.profile-logout:active {
  background: #FEE;
}
</style>
