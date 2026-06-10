<template>
  <div class="reviews-page">
    <div class="header-gradient" style="font-size:18px;">💬 评价管理</div>

    <!-- 评分分布 -->
    <div class="card" style="margin:10px 12px;" v-if="ratingDist">
      <div v-for="i in 5" :key="i" class="rv-dist-item">
        <span class="rv-dist-label">{{ 6-i }}星</span>
        <el-progress
          :percentage="Math.round((ratingDist[6-i] || 0) / Math.max(1, reviews.length) * 100)"
          :stroke-width="12"
          style="flex:1;"
        />
        <span class="rv-dist-count">{{ ratingDist[6-i] || 0 }}</span>
      </div>
    </div>

    <!-- 评价列表 -->
    <div style="padding:0 12px;">
      <div v-for="r in reviews" :key="r.id" class="card" style="margin:0 0 10px;">
        <div class="rv-header">
          <el-rate :model-value="roundedStars[r.id] || 0" disabled size="small" />
          <span class="rv-user">{{ r.consumer?.username || '匿名' }}</span>
        </div>
        <p class="rv-comment" v-if="r.comment">{{ r.comment }}</p>
        <div class="rv-reply" v-if="r.reply">
          <strong>商家回复：</strong>{{ r.reply }}
        </div>
        <div v-else class="rv-reply-box">
          <el-input v-model="replyTexts[r.id]" placeholder="回复顾客评价..." size="small">
            <template #append>
              <el-button @click="replyReview(r.id)" :loading="replyLoading[r.id]">回复</el-button>
            </template>
          </el-input>
        </div>
      </div>
      <el-empty v-if="!reviews.length" description="暂无评价" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { merchantApi } from '@/api/merchant'

const reviews = ref<any[]>([])
const ratingDist = ref<number[]>([])
const roundedStars = ref<Record<number, number>>({})
const replyTexts = reactive<Record<number, string>>({})
const replyLoading = reactive<Record<number, boolean>>({})

async function load() {
  const res: any = await merchantApi.getReviews()
  reviews.value = res.reviews || []
  ratingDist.value = res.ratingDist || []
  roundedStars.value = res.roundedStars || {}
  ;(res.reviews || []).forEach((r: any) => {
    if (!replyTexts[r.id]) replyTexts[r.id] = ''
  })
}
async function replyReview(id: number) {
  if (!replyTexts[id]) { ElMessage.warning('请输入回复内容'); return }
  replyLoading[id] = true
  try { await merchantApi.replyReview(id, replyTexts[id]); ElMessage.success('回复成功'); load() }
  finally { replyLoading[id] = false }
}
onMounted(load)
</script>

<style scoped>
.rv-dist-item { display: flex; align-items: center; gap: 8px; margin: 4px 0; font-size: 13px; }
.rv-dist-label { width: 28px; text-align: right; color: #999; }
.rv-dist-count { width: 20px; text-align: center; color: #999; }
.rv-header { display: flex; align-items: center; gap: 8px; }
.rv-user { font-size: 12px; color: #999; }
.rv-comment { margin: 8px 0; font-size: 14px; }
.rv-reply { background: #FFF3CD; padding: 8px; border-radius: 8px; font-size: 13px; }
.rv-reply-box { margin-top: 8px; }
</style>
