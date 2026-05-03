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
      <div class="stat-card stat-card--video">
        <div class="stat-card__icon">🎬</div>
        <div class="stat-card__body">
          <div class="stat-card__value">{{ stats.videoCount }}</div>
          <div class="stat-card__label">已发布视频</div>
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

      <!-- 快捷操作 -->
      <div class="card-surface">
        <h3 class="section-title">快捷操作</h3>
        <div class="quick-actions">
          <router-link to="/news" class="quick-btn quick-btn--news">
            <span class="quick-btn__icon">✏️</span>
            <span>管理新闻</span>
          </router-link>
          <router-link to="/videos" class="quick-btn quick-btn--video">
            <span class="quick-btn__icon">🎬</span>
            <span>管理视频</span>
          </router-link>
          <router-link to="/submissions" class="quick-btn quick-btn--pending">
            <span class="quick-btn__icon">📋</span>
            <span>审核投稿</span>
            <el-badge v-if="stats.pendingSubmissions > 0" :value="stats.pendingSubmissions" class="quick-badge" />
          </router-link>
          <router-link to="/media" class="quick-btn quick-btn--media">
            <span class="quick-btn__icon">🖼️</span>
            <span>媒体资源</span>
          </router-link>
          <router-link to="/comments" class="quick-btn quick-btn--comment">
            <span class="quick-btn__icon">💬</span>
            <span>评论管理</span>
          </router-link>
        </div>
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
import { reactive, onMounted } from 'vue'
import { fetchDashboard } from '../api/modules'

const stats = reactive({
  newsCount: 0,
  videoCount: 0,
  pendingSubmissions: 0,
  todayOperations: 0,
  categories: [],
  recentNews: [],
  recentSubmissions: []
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
    // leave defaults
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
.stat-card--video .stat-card__icon { background: #fef3c7; }
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

/* 快捷操作 */
.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.15s;
  border: 1px solid var(--line);
  color: var(--ink-1);
  background: #fff;
}

.quick-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}

.quick-btn__icon { font-size: 18px; }

.quick-btn--news:hover { border-color: #e24b4b; color: #e24b4b; }
.quick-btn--video:hover { border-color: #f59e0b; color: #f59e0b; }
.quick-btn--pending:hover { border-color: #22b573; color: #22b573; }
.quick-btn--media:hover { border-color: #667eea; color: #667eea; }
.quick-btn--comment:hover { border-color: #0ea5a4; color: #0ea5a4; }

.quick-badge {
  margin-left: 4px;
}
</style>
