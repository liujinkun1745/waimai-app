<template>
  <div class="login-page">
    <!-- 黄色渐变背景装饰 -->
    <div class="login-bg"></div>

    <div class="login-content">
      <!-- Logo 区 -->
      <div class="login-logo-area">
        <div class="login-logo">🛵</div>
        <h1 class="login-title">美味外卖</h1>
        <p class="login-subtitle">美食即刻送达</p>
      </div>

      <!-- 登录表单 -->
      <el-form ref="formRef" :model="form" :rules="rules" size="large" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名 / 手机号"
            :prefix-icon="User"
            size="large"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            size="large"
            class="login-input"
          />
        </el-form-item>
        <el-button
          color="#FFB800"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
          round
          size="large"
        >
          登 录
        </el-button>
      </el-form>

      <!-- 注册链接 -->
      <div class="login-link">
        还没有账号？
        <router-link to="/register" class="login-reg-link">立即注册 →</router-link>
      </div>

      <!-- 测试账号快捷填充 -->
      <div class="login-test">
        <p class="login-test-title">🔑 测试账号（点击快速填充）</p>
        <div class="login-test-list">
          <span
            v-for="a in testAccounts"
            :key="a.user"
            class="login-test-tag"
            @click="fillTest(a)"
          >
            {{ a.label }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const formRef = ref()

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const testAccounts = [
  { user: 'consumer1', password: '123456', label: '🧑 消费者 consumer1' },
  { user: 'merchant1', password: '123456', label: '🏪 商家 merchant1' },
]

function fillTest(a: { user: string; password: string }) {
  form.username = a.user
  form.password = a.password
  ElMessage.success(`已填充：${a.user}`)
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await store.login(form)
    ElMessage.success('登录成功！')
    const role = localStorage.getItem('role')
    router.push(role === 'ROLE_CONSUMER' ? '/consumer/home' : '/merchant/orders')
  } catch {
    ElMessage.error('用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--mt-bg);
  overflow: hidden;
}

/* 黄色渐变背景 */
.login-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 45%;
  background: linear-gradient(180deg, #FFD101 0%, #FFE44D 60%, #FFF8E1 100%);
  border-radius: 0 0 40px 40px;
}

.login-content {
  position: relative;
  z-index: 1;
  width: 100%;
  padding: 24px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* Logo */
.login-logo-area {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  font-size: 72px;
  animation: logoBounce 1s ease infinite alternate;
}

@keyframes logoBounce {
  from { transform: translateY(0); }
  to   { transform: translateY(-6px); }
}

.login-title {
  font-size: 30px;
  font-weight: 800;
  color: #1A1A1A;
  margin-top: 8px;
}

.login-subtitle {
  color: #666;
  font-size: 14px;
  margin-top: 4px;
  letter-spacing: 2px;
}

/* 表单 */
.login-form {
  width: 100%;
}

.login-input :deep(.el-input__wrapper) {
  border-radius: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.login-btn {
  width: 100%;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 6px;
  height: 48px;
  margin-top: 6px;
  box-shadow: 0 4px 12px rgba(255, 184, 0, 0.4);
}

/* 注册链接 */
.login-link {
  margin-top: 18px;
  font-size: 14px;
  color: #666;
}

.login-reg-link {
  color: #FF6B35;
  font-weight: 600;
}

/* 测试账号 */
.login-test {
  margin-top: 28px;
  text-align: center;
}

.login-test-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 10px;
}

.login-test-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.login-test-tag {
  display: inline-block;
  padding: 6px 16px;
  background: #FFF3CD;
  border-radius: 20px;
  font-size: 13px;
  color: #FFB800;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.15s;
  border: 1px solid transparent;
}

.login-test-tag:hover {
  background: #FFE69C;
  border-color: #FFB800;
}
</style>
