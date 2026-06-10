<template>
  <div class="earnings-page">
    <!-- 头部 -->
    <div class="header-gradient" style="font-size:18px;">📊 收益统计</div>

    <!-- 统计卡片 2x2 -->
    <div class="ev-stats">
      <div class="ev-stat-card">
        <b>¥{{ data.todayEarnings }}</b>
        <span>今日收益</span>
      </div>
      <div class="ev-stat-card">
        <b>¥{{ data.weekEarnings }}</b>
        <span>本周收益</span>
      </div>
      <div class="ev-stat-card">
        <b>¥{{ data.monthEarnings }}</b>
        <span>本月收益</span>
      </div>
      <div class="ev-stat-card">
        <b>¥{{ data.totalEarnings }}</b>
        <span>累计收益</span>
      </div>
    </div>

    <!-- 订单统计条 -->
    <div class="ev-order-bar">
      <span>今日 {{ data.todayOrders }} 单</span>
      <span>本周 {{ data.weekOrders }} 单</span>
      <span>本月 {{ data.monthOrders }} 单</span>
      <span>累计 {{ data.totalOrders }} 单</span>
    </div>

    <!-- 7天趋势图 -->
    <div class="card ev-chart-card" style="margin:10px 12px;">
      <div class="ev-chart-title">近7天收益趋势</div>
      <div style="height:200px;">
        <canvas id="chart7"></canvas>
      </div>
    </div>

    <!-- 30天趋势图 -->
    <div class="card ev-chart-card" style="margin:10px 12px;">
      <div class="ev-chart-title">近30天收益趋势</div>
      <div style="height:200px;">
        <canvas id="chart30"></canvas>
      </div>
    </div>

    <div style="height:16px;"></div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, nextTick } from 'vue'
import { Chart, LineController, LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip, Legend } from 'chart.js'
import { merchantApi } from '@/api/merchant'

Chart.register(LineController, LineElement, PointElement, LinearScale, CategoryScale, Filler, Tooltip, Legend)

const data = reactive({
  todayEarnings: 0, weekEarnings: 0, monthEarnings: 0, totalEarnings: 0,
  todayOrders: 0, weekOrders: 0, monthOrders: 0, totalOrders: 0,
  chart7Labels: [] as string[],
  chart7Revenue: [] as string[],
  chart30Labels: [] as string[],
  chart30Revenue: [] as string[],
})

async function load() {
  const res: any = await merchantApi.getEarnings()
  Object.assign(data, res)

  await nextTick()
  renderChart('chart7', data.chart7Labels, data.chart7Revenue, '#FFD101')
  renderChart('chart30', data.chart30Labels, data.chart30Revenue, '#FFB800')
}

function renderChart(canvasId: string, labels: string[], revenue: string[], color: string) {
  const canvas = document.getElementById(canvasId) as HTMLCanvasElement | null
  if (!canvas) return

  // 销毁旧实例
  const existing = Chart.getChart(canvasId)
  if (existing) existing.destroy()

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 创建渐变填充
  const gradient = ctx.createLinearGradient(0, 0, 0, 200)
  gradient.addColorStop(0, color + '40')
  gradient.addColorStop(1, color + '05')

  new Chart(canvas, {
    type: 'line',
    data: {
      labels,
      datasets: [{
        label: '收益 (¥)',
        data: revenue.map(Number),
        borderColor: color,
        backgroundColor: gradient,
        fill: true,
        tension: 0.4,
        pointRadius: labels.length > 7 ? 0 : 3,
        pointBackgroundColor: color,
        borderWidth: 2,
      }],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#FFF',
          titleColor: '#333',
          bodyColor: '#FF6B35',
          borderColor: '#F0F0F0',
          borderWidth: 1,
          cornerRadius: 8,
          displayColors: false,
        },
      },
      scales: {
        x: {
          grid: { display: false },
          ticks: { color: '#BBB', font: { size: 11 } },
        },
        y: {
          beginAtZero: true,
          grid: { color: '#F5F5F5' },
          ticks: {
            color: '#BBB',
            font: { size: 11 },
            callback: (v) => '¥' + v,
          },
        },
      },
    },
  })
}

onMounted(load)
</script>

<style scoped>
.earnings-page {
  min-height: 100vh;
  background: #F5F5F5;
}

/* 统计卡片 */
.ev-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 14px;
}

.ev-stat-card {
  background: #FFF;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
  border-top: 3px solid #FFD101;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.ev-stat-card b {
  display: block;
  font-size: 24px;
  font-weight: 800;
  color: #FF6B35;
}

.ev-stat-card span {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  display: block;
}

/* 订单统计条 */
.ev-order-bar {
  display: flex;
  justify-content: space-around;
  padding: 0 14px 10px;
  font-size: 13px;
  color: #666;
}

/* 图表 */
.ev-chart-card {
  padding: 14px;
}

.ev-chart-title {
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 10px;
}
</style>
