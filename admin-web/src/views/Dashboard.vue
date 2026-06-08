<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">控制台总览</h1>
        <p class="page-subtitle">内容运营数据一目了然</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div class="stat-card stat-card--news">
        <div class="stat-card__icon">📰</div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.newsCount }}</div>
          <div class="stat-card__label">已发布新闻</div>
        </div>
      </div>
      <div class="stat-card stat-card--pending">
        <div class="stat-card__icon">📝</div>
        <div class="stat-card__body">
          <div class="stat-card__value" :class="{ 'text-warning': stats.pendingSubmissions > 0 }">{{ stats.pendingSubmissions }}</div>
          <div class="stat-card__label">待审核投稿</div>
        </div>
      </div>
      <div class="stat-card stat-card--ops">
        <div class="stat-card__icon">📊</div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.todayOperations }}</div>
          <div class="stat-card__label">今日操作量</div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <!-- 分类分布 -->
      <div class="card-surface">
        <h3 class="section-title">新闻分类分布</h3>
        <div v-if="stats.categories && stats.categories.length > 0" class="category-list">
          <div class="category-row" v-for="cat in stats.categories" :key="cat.name">
            <span class="category-name">{{ cat.name }}</span>
            <div class="category-bar-wrap">
              <div class="category-bar" :style="{ width: barWidth(cat.count) }"></div>
            </div>
            <span class="category-count">{{ cat.count }}篇</span>
          </div>
        </div>
        <div v-else class="empty-hint">暂无数据</div>
      </div>

      <!-- 分类占比环形图 -->
      <div class="card-surface">
        <h3 class="section-title">新闻分类占比</h3>
        <div class="donut-wrap" v-if="stats.categories && stats.categories.length > 0">
          <div class="donut-chart">
            <svg viewBox="0 0 200 200">
              <circle cx="100" cy="100" r="78" fill="none" stroke="#f0f2f5" stroke-width="26"/>
              <circle
                v-for="(cat, idx) in donutSegments"
                :key="cat.name"
                cx="100" cy="100" r="78"
                fill="none"
                :stroke="cat.color"
                stroke-width="26"
                :stroke-dasharray="cat.dashArray"
                :stroke-dashoffset="cat.dashOffset"
                stroke-linecap="butt"
                :transform="cat.transform"
                style="transition: all 0.8s ease"
              />
            </svg>
            <div class="donut-center">
              <div class="donut-total">{{ totalNews }}</div>
              <div class="donut-label">篇新闻</div>
            </div>
          </div>
          <div class="donut-legend">
            <span class="legend-item" v-for="(cat, idx) in topCategories" :key="cat.name">
              <span class="legend-dot" :style="{ background: cat.color }"></span>
              <span class="legend-name">{{ cat.name }}</span>
              <span class="legend-value">{{ cat.count }}</span>
            </span>
          </div>
        </div>
        <div v-else class="empty-hint">暂无数据</div>
      </div>

      <!-- 最新新闻 -->
      <div class="card-surface">
        <h3 class="section-title">最新发布新闻</h3>
        <el-table :data="stats.recentNews || []" size="small" v-if="stats.recentNews && stats.recentNews.length > 0">
          <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="sourceName" label="来源" width="120" />
          <el-table-column prop="publishedAt" label="发布时间" width="160" />
        </el-table>
        <div v-else class="empty-hint">暂无新闻</div>
      </div>

      <!-- 待审核投稿 -->
      <div class="card-surface">
        <h3 class="section-title">
          待审核投稿
          <el-badge v-if="stats.pendingSubmissions > 0" :value="stats.pendingSubmissions" style="margin-left:8px" />
        </h3>
        <el-table :data="stats.recentSubmissions || []" size="small" v-if="stats.recentSubmissions && stats.recentSubmissions.length > 0">
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="提交时间" width="160" />
        </el-table>
        <div v-else class="empty-hint">暂无待审核投稿 🎉</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchDashboard } from '../api/modules'

const DONUT_COLORS = [
  '#7c3aed', '#ef4444', '#f59e0b', '#10b981', '#3b82f6',
  '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16', '#f97316',
  '#6366f1', '#14b8a6', '#e11d48', '#0ea5e9'
]

const stats = reactive({
  newsCount: 0,
  videoCount: 0,
  pendingSubmissions: 0,
  todayOperations: 0,
  categories: [],
  recentNews: [],
  recentSubmissions: []
})

const totalNews = computed(() => stats.categories.reduce((s, c) => s + c.count, 0))

const topCategories = computed(() => {
  return [...stats.categories]
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
    .map((c, i) => ({ ...c, color: DONUT_COLORS[i % DONUT_COLORS.length] }))
})

const donutSegments = computed(() => {
  const total = Math.max(totalNews.value, 1)
  const circumference = 2 * Math.PI * 78
  const sorted = [...stats.categories].sort((a, b) => b.count - a.count).slice(0, 10)
  let cumulative = 0
  return sorted.map((cat, i) => {
    const ratio = cat.count / total
    const len = Math.max(ratio * circumference, 0.5)
    const seg = {
      name: cat.name,
      color: DONUT_COLORS[i % DONUT_COLORS.length],
      dashArray: `${len} ${circumference - len}`,
      dashOffset: -cumulative,
      transform: `rotate(-90 100 100)`
    }
    cumulative += len
    return seg
  })
})

const barWidth = (count) => {
  const max = Math.max(...stats.categories.map(c => c.count), 1)
  return ((count / max) * 100).toFixed(0) + '%'
}

onMounted(async () => {
  try {
    const data = await fetchDashboard()
    Object.assign(stats, data)
  } catch (e) {
    ElMessage.error('加载仪表盘数据失败，请检查后端服务')
  }
})
</script>

<style scoped>
.dashboard-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 1100px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
}

.empty-hint {
  text-align: center;
  color: #bbb;
  padding: 32px 0;
  font-size: 14px;
}

/* 统计卡片 */
.stat-card {
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid var(--line);
  background: #fff;
}

.stat-card__icon {
  font-size: 36px;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card--news .stat-card__icon { background: #eef2ff; }
.stat-card--pending .stat-card__icon { background: #fef2f2; }
.stat-card--ops .stat-card__icon { background: #ecfdf5; }

.stat-card__value {
  font-size: 28px;
  font-weight: 800;
  color: #1a1a2e;
}

.stat-card__label {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}

.text-warning { color: #e6a23c !important; }

/* 分类分布 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-name {
  width: 80px;
  font-size: 13px;
  color: #555;
  flex-shrink: 0;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-bar-wrap {
  flex: 1;
  height: 18px;
  background: #f0f2f5;
  border-radius: 9px;
  overflow: hidden;
}

.category-bar {
  height: 100%;
  border-radius: 9px;
  background: linear-gradient(90deg, #e24b4b, #f59e0b);
  min-width: 2px;
  transition: width 0.6s ease;
}

.category-count {
  font-size: 13px;
  color: #888;
  width: 40px;
  flex-shrink: 0;
}

/* 环形图 */
.donut-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.donut-chart {
  position: relative;
  width: 210px;
  height: 210px;
  flex-shrink: 0;
}

.donut-chart svg {
  width: 100%;
  height: 100%;
}

.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.donut-total {
  font-size: 38px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1;
}

.donut-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.donut-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px 16px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-name {
  color: #555;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.legend-value {
  font-weight: 600;
  color: #888;
}
</style>
