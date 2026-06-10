<template>
  <div class="balance-page">
    <div class="header-gradient" style="gap:12px;">
      <el-icon :size="20" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span style="font-size:18px;font-weight:700;">我的余额</span>
    </div>
    <div style="text-align:center;padding:32px 16px;background:#FFF;">
      <div style="font-size:14px;color:#666;">账户余额</div>
      <div style="font-size:42px;color:#FF6B35;font-weight:800;margin:8px 0 16px;">¥{{ balance }}</div>
      <el-button type="warning" size="large" round @click="showRecharge = true">充 值</el-button>
    </div>
    <div style="padding:14px;">
      <h3 style="margin-bottom:12px;font-size:15px;">变动记录</h3>
      <div v-for="r in records" :key="r.id" class="b-rec">
        <div>
          <div class="b-rec-desc">{{ r.description }}</div>
          <div class="b-rec-time">{{ r.createdAt?.substring(0, 19) }}</div>
        </div>
        <span :class="r.type === '充值' ? 'b-income' : 'b-expense'">
          {{ r.type === '充值' ? '+' : '' }}¥{{ r.amount }}
        </span>
      </div>
      <el-empty v-if="!records.length" description="暂无记录" :image-size="50" />
    </div>

    <el-dialog v-model="showRecharge" title="余额充值" width="80%">
      <el-input v-model="rechargeAmount" placeholder="充值金额" type="number" size="large">
        <template #prefix>¥</template>
      </el-input>
      <template #footer>
        <el-button @click="showRecharge = false">取消</el-button>
        <el-button type="warning" @click="handleRecharge" :loading="recharging">确认充值</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'

const balance = ref(0)
const records = ref<any[]>([])
const showRecharge = ref(false)
const recharging = ref(false)
const rechargeAmount = ref('')

async function load() {
  const res: any = await consumerApi.getBalance()
  balance.value = res?.balance || 0
  records.value = res?.records || []
}
async function handleRecharge() {
  const amount = parseFloat(rechargeAmount.value)
  if (isNaN(amount) || amount <= 0) { ElMessage.error('请输入有效金额'); return }
  recharging.value = true
  try {
    await consumerApi.recharge({ amount })
    ElMessage.success('充值成功')
    showRecharge.value = false
    rechargeAmount.value = ''
    load()
  } finally { recharging.value = false }
}
onMounted(load)
</script>

<style scoped>
.b-rec { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #F0F0F0; }
.b-rec-desc { font-size: 14px; }
.b-rec-time { font-size: 12px; color: #999; }
.b-income { color: #52C41A; font-weight: 700; }
.b-expense { color: #FF4D4F; font-weight: 700; }
</style>
