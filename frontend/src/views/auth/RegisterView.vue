<template>
  <div class="register-page">
    <div class="register-top">
      <el-button text @click="$router.push('/login')">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>注册账号</h2>
      <div style="width:60px;"></div>
    </div>

    <el-tabs v-model="activeTab" stretch class="register-tabs">
      <el-tab-pane label="消费者注册" name="consumer" />
      <el-tab-pane label="商家注册" name="merchant" />
    </el-tabs>

    <!-- 消费者注册 -->
    <el-form v-if="activeTab === 'consumer'" ref="cFormRef" :model="cForm" :rules="cRules" size="large" class="register-form">
      <el-form-item prop="username">
        <el-input v-model="cForm.username" placeholder="用户名（3-50位）" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="cForm.phone" placeholder="手机号" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="cForm.password" type="password" placeholder="密码（6-50位）" show-password />
      </el-form-item>
      <el-form-item>
        <el-input v-model="cForm.email" placeholder="邮箱（选填）" />
      </el-form-item>
      <el-form-item>
        <el-button type="warning" class="register-btn" :loading="cLoading" @click="handleConsumerRegister">注 册</el-button>
      </el-form-item>
    </el-form>

    <!-- 商家注册 -->
    <el-form v-if="activeTab === 'merchant'" ref="mFormRef" :model="mForm" :rules="mRules" size="large" class="register-form">
      <el-form-item prop="username">
        <el-input v-model="mForm.username" placeholder="用户名（3-50位）" />
      </el-form-item>
      <el-form-item prop="phone">
        <el-input v-model="mForm.phone" placeholder="手机号" />
      </el-form-item>
      <el-form-item prop="password">
        <el-input v-model="mForm.password" type="password" placeholder="密码（6-50位）" show-password />
      </el-form-item>
      <el-form-item prop="shopName">
        <el-input v-model="mForm.shopName" placeholder="店铺名称" />
      </el-form-item>
      <el-form-item prop="shopAddress">
        <el-input v-model="mForm.shopAddress" placeholder="店铺地址" />
      </el-form-item>
      <el-form-item prop="businessLicense">
        <el-input v-model="mForm.businessLicense" placeholder="营业执照号" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="mForm.description" placeholder="店铺简介（选填）" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="warning" class="register-btn" :loading="mLoading" @click="handleMerchantRegister">注 册</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()
const activeTab = ref('consumer')
const cLoading = ref(false), mLoading = ref(false)
const cFormRef = ref(), mFormRef = ref()

const cForm = reactive({ username: '', phone: '', password: '', email: '' })
const cRules = {
  username: [{ required: true, min: 3, max: 50, message: '用户名3-50位', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 50, message: '密码6-50位', trigger: 'blur' }],
}

const mForm = reactive({
  username: '', phone: '', password: '', shopName: '',
  shopAddress: '', businessLicense: '', description: '',
})
const mRules = {
  username: [{ required: true, min: 3, max: 50, message: '用户名3-50位', trigger: 'blur' }],
  phone: [{ required: true, pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 50, message: '密码6-50位', trigger: 'blur' }],
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  shopAddress: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }],
  businessLicense: [{ required: true, message: '请输入营业执照号', trigger: 'blur' }],
}

async function handleConsumerRegister() {
  const valid = await cFormRef.value?.validate().catch(() => false)
  if (!valid) return
  cLoading.value = true
  try {
    await authApi.registerConsumer(cForm)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch { /* handled */ }
  finally { cLoading.value = false }
}

async function handleMerchantRegister() {
  const valid = await mFormRef.value?.validate().catch(() => false)
  if (!valid) return
  mLoading.value = true
  try {
    await authApi.registerMerchant(mForm)
    ElMessage.success('商家注册成功，请登录')
    router.push('/login')
  } catch { /* handled */ }
  finally { mLoading.value = false }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: #FFF;
}

.register-top {
  display: flex;
  align-items: center;
  padding: 12px 16px;
}

.register-top h2 {
  flex: 1;
  text-align: center;
  font-size: 18px;
}

.register-tabs {
  background: #FFF;
}

.register-form {
  padding: 16px 20px;
}

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 4px;
}
</style>
