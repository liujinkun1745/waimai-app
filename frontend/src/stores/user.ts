import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi, type LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<any>(JSON.parse(localStorage.getItem('user') || 'null'))
  const token = ref(localStorage.getItem('accessToken') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isConsumer = computed(() => user.value?.role === 'ROLE_CONSUMER')
  const isMerchant = computed(() => user.value?.role === 'ROLE_MERCHANT')

  async function login(params: LoginParams) {
    const res: any = await authApi.login(params)
    localStorage.setItem('accessToken', res.accessToken)
    localStorage.setItem('refreshToken', res.refreshToken)
    localStorage.setItem('role', res.role)
    token.value = res.accessToken
    user.value = { id: res.userId, username: res.username, role: res.role, phone: res.phone }
    localStorage.setItem('user', JSON.stringify(user.value))
    return res
  }

  function logout() {
    localStorage.clear()
    user.value = null
    token.value = ''
  }

  return { user, token, isLoggedIn, isConsumer, isMerchant, login, logout }
})
