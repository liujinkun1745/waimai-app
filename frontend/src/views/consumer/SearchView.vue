<template>
  <div class="search-page">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索商家"
        size="large"
        @keyup.enter="doSearch()"
        clearable
        :prefix-icon="Search"
      />
      <el-button type="warning" @click="doSearch()">搜索</el-button>
    </div>

    <!-- 搜索结果 -->
    <div class="search-results" v-if="searched">
      <div v-for="m in merchants" :key="m.id" class="card search-card"
           style="margin:0 12px 10px;cursor:pointer;"
           @click="$router.push(`/consumer/merchant/${m.id}`)">
        <div class="sc-row">
          <span class="sc-avatar">{{ getEmoji(m.shopName) }}</span>
          <div class="sc-info">
            <div class="sc-name">{{ m.shopName }}</div>
            <el-rate :model-value="m.rating" disabled show-score size="small" />
            <div class="sc-meta">月售{{ m.monthlySales }} · ¥{{ m.deliveryFee }}配送</div>
          </div>
        </div>
      </div>
      <el-empty v-if="!merchants.length" description="未找到商家" />
    </div>

    <!-- 未搜索时：搜索历史 + 热门 -->
    <div v-else style="padding:14px;">
      <!-- 搜索历史 -->
      <div v-if="history.length" class="search-history">
        <div class="sh-title">
          <span>🕐 最近搜索</span>
          <span class="sh-clear" @click="clearHistory">清空</span>
        </div>
        <div class="sh-tags">
          <span
            v-for="(h, i) in history"
            :key="i"
            class="sh-tag"
            @click="keyword = h; doSearch()"
          >{{ h }}</span>
        </div>
      </div>

      <h3 style="margin:16px 0 12px;">🔥 热门商家</h3>
      <div v-for="m in hotMerchants" :key="m.id" class="card"
           style="margin:0 0 10px;cursor:pointer;"
           @click="$router.push(`/consumer/merchant/${m.id}`)">
        <div class="sc-row">
          <span class="sc-avatar">{{ getEmoji(m.shopName) }}</span>
          <span class="sc-name">{{ m.shopName }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { consumerApi } from '@/api/consumer'

const route = useRoute()
const keyword = ref('')
const merchants = ref<any[]>([])
const hotMerchants = ref<any[]>([])
const searched = ref(false)
const STORAGE_KEY = 'waimai_search_history'

const history = ref<string[]>(JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'))

const emojiMap: Record<string, string> = {
  '美味家常菜': '🍜', '甜蜜时光烘焙': '🍰', '鲜果鲜生': '🍎',
  '深夜烧烤': '🍖', '瑞幸咖啡': '☕', '麦香鸡汉堡': '🍔',
  '幸福西饼': '🎂', '天天超市': '🏪',
}
function getEmoji(name: string) { return emojiMap[name] || '🏪' }

function saveHistory(k: string) {
  if (!k.trim()) return
  const h = history.value.filter(h => h !== k)
  h.unshift(k)
  history.value = h.slice(0, 8)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(history.value))
}

function clearHistory() {
  history.value = []
  localStorage.removeItem(STORAGE_KEY)
}

async function doSearch(kw?: string) {
  const term = (kw || keyword.value).trim()
  if (!term) return
  keyword.value = term
  searched.value = true
  saveHistory(term)
  const res: any = await consumerApi.search(term)
  merchants.value = res?.merchants || []
}
onMounted(async () => {
  const res: any = await consumerApi.search()
  hotMerchants.value = res?.hotMerchants || []
  // 从首页分类图标跳过来时带 keyword 参数，自动搜索
  const q = route.query.keyword as string
  if (q) await doSearch(q)
})
</script>

<style scoped>
.search-bar { display: flex; gap: 8px; padding: 12px 14px; background: linear-gradient(180deg, #FFD101, #FFB800); }
.sc-row { display: flex; gap: 12px; align-items: center; }
.sc-avatar { width: 44px; height: 44px; border-radius: 8px; background: #FFF8E1; display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
.sc-name { font-weight: 700; font-size: 15px; }
.sc-info { flex: 1; }
.sc-meta { font-size: 12px; color: #999; margin-top: 2px; }

/* 搜索历史 */
.search-history { margin-bottom: 4px; }
.sh-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 15px; font-weight: 700; }
.sh-clear { font-size: 13px; color: #999; font-weight: 400; cursor: pointer; }
.sh-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.sh-tag {
  display: inline-block;
  padding: 6px 14px;
  background: #F5F5F5;
  border-radius: 16px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}
.sh-tag:hover { background: #FFF3CD; color: #FFB800; }
</style>
