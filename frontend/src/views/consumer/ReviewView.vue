<template>
  <div class="review-page">
    <div class="header-gradient" style="gap:12px;">
      <el-icon :size="20" @click="$router.back()" style="cursor:pointer;"><ArrowLeft /></el-icon>
      <span style="font-size:18px;font-weight:700;">评价订单</span>
    </div>
    <div style="padding:20px 16px;">
      <div class="rv-group"><span>口味</span><el-rate v-model="form.tasteRating" :max="5" /></div>
      <div class="rv-group"><span>包装</span><el-rate v-model="form.packagingRating" :max="5" /></div>
      <div class="rv-group"><span>配送</span><el-rate v-model="form.deliveryRating" :max="5" /></div>
      <el-input v-model="form.comment" type="textarea" :rows="3" placeholder="说说你的感受吧（选填）" style="margin-bottom:20px;" />
      <el-button type="warning" style="width:100%;height:46px;font-size:16px;font-weight:700;" :loading="submitting" @click="handleSubmit">提交评价</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { consumerApi } from '@/api/consumer'
const route = useRoute(); const router = useRouter(); const submitting = ref(false)
const form = reactive({ tasteRating: 5, packagingRating: 5, deliveryRating: 5, comment: '' })
const orderId = Number(route.params.id)
async function handleSubmit() {
  if (form.tasteRating === 0 || form.packagingRating === 0 || form.deliveryRating === 0) { ElMessage.warning('请完成所有评分'); return }
  submitting.value = true
  try { await consumerApi.submitReview(orderId, form); ElMessage.success('评价成功！'); router.push('/consumer/orders') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.rv-group { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; font-size: 15px; }
</style>
