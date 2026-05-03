<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">媒体资源库</h1>
        <p class="page-subtitle">上传和管理图片、视频素材，供新闻封面和内容使用</p>
      </div>
    </div>

    <!-- 用途说明卡片 -->
    <el-alert
      title="媒体资源库的作用"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:16px; border-radius:12px"
    >
      <template #default>
        <div class="usage-text">
          <p><strong>1. 上传图片：</strong>点击下方"上传文件"按钮，上传图片（如新闻封面、配图）。上传后会返回一个 URL。</p>
          <p><strong>2. 复制 URL：</strong>点击"复制 URL"将图片地址复制到剪贴板。</p>
          <p><strong>3. 粘贴使用：</strong>在<strong>新闻管理 → 编辑新闻 → 封面地址</strong>中粘贴 URL，或点击新闻编辑页的"上传"按钮直接上传。</p>
          <p><strong>4. 管理素材：</strong>所有已上传的素材在此集中展示，方便查找和重复使用。</p>
        </div>
      </template>
    </el-alert>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-upload
          :before-upload="handleUpload"
          :show-file-list="false"
          accept="image/*,video/mp4"
          drag
        >
          <div class="upload-zone">
            <div class="upload-icon">📤</div>
            <div class="upload-text">点击或拖拽上传图片/视频</div>
            <div class="upload-hint">支持 JPG、PNG、GIF、WebP、MP4</div>
          </div>
        </el-upload>
        <span class="item-count">已上传 {{ uploads.length }} 个文件</span>
      </div>

      <div v-if="uploads.length === 0" class="empty-hint">
        <div class="empty-icon-large">🖼️</div>
        <div>暂无媒体资源</div>
        <div class="empty-sub">上传图片或视频后，复制 URL 即可在新闻/视频管理中使用</div>
      </div>

      <div class="media-grid" v-else>
        <div class="media-card" v-for="(item, idx) in uploads" :key="idx">
          <div class="media-preview">
            <img v-if="isImage(item.url)" :src="item.url" class="media-thumb" />
            <video v-else-if="isVideo(item.url)" :src="item.url" class="media-thumb" controls />
            <div v-else class="media-unknown">📎</div>
          </div>
          <div class="media-info">
            <div class="media-name">{{ item.name }}</div>
            <div class="media-type-tag">{{ fileTypeLabel(item.url) }}</div>
            <div class="media-time">{{ item.time }}</div>
          </div>
          <div class="media-url-row" :title="item.url">{{ item.url }}</div>
          <div class="media-actions">
            <el-button size="small" type="primary" @click="copyUrl(item.url)">复制 URL</el-button>
            <el-button size="small" type="danger" @click="removeItem(idx)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 使用提示 -->
    <div class="card-surface" style="margin-top:16px">
      <h3 class="section-title">💡 使用场景示例</h3>
      <div class="scenario-grid">
        <div class="scenario-card">
          <div class="scenario-step">①</div>
          <div>在媒体库<strong>上传封面图</strong></div>
          <div class="scenario-arrow">→</div>
          <div>复制图片 URL</div>
          <div class="scenario-arrow">→</div>
          <div>到 <router-link to="/news">新闻管理</router-link> 编辑时<strong>粘贴到封面地址</strong></div>
        </div>
        <div class="scenario-card">
          <div class="scenario-step">②</div>
          <div>在媒体库<strong>上传视频</strong></div>
          <div class="scenario-arrow">→</div>
          <div>复制视频 URL</div>
          <div class="scenario-arrow">→</div>
          <div>到 <router-link to="/videos">视频管理</router-link> 新建时<strong>粘贴到视频地址</strong></div>
        </div>
        <div class="scenario-card">
          <div class="scenario-step">③</div>
          <div>在 <router-link to="/news">新闻管理</router-link> 编辑新闻时</div>
          <div class="scenario-arrow">→</div>
          <div>点击封面地址旁的<strong>"上传"按钮</strong></div>
          <div class="scenario-arrow">→</div>
          <div>直接上传，URL<strong>自动填入</strong>，同时出现在本页面</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadFile } from '../api/modules'

const uploads = reactive([])

const isImage = (url) => /\.(jpg|jpeg|png|gif|webp|svg)(\?|$)/i.test(url)
const isVideo = (url) => /\.(mp4|webm|mov)(\?|$)/i.test(url)

const fileTypeLabel = (url) => {
  if (isImage(url)) return '图片'
  if (isVideo(url)) return '视频'
  return '文件'
}

const handleUpload = async (file) => {
  try {
    const data = await uploadFile(file)
    const url = data && data.url
    if (url) {
      uploads.unshift({ url, name: file.name, time: new Date().toLocaleString() })
      ElMessage.success('上传成功！复制 URL 后可在新闻/视频管理中使用')
    }
  } catch (e) {
    ElMessage.error((e && e.message) || '上传失败')
  }
  return false
}

const copyUrl = (url) => {
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('URL 已复制到剪贴板')
  }).catch(() => {
    ElMessage.info(url)
  })
}

const removeItem = (idx) => {
  uploads.splice(idx, 1)
}
</script>

<style scoped>
.usage-text p {
  margin: 4px 0;
  line-height: 1.7;
  font-size: 13px;
}

.section-title {
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 700;
}

.item-count {
  color: #999;
  font-size: 13px;
  margin-left: 16px;
}

.upload-zone {
  text-align: center;
  padding: 24px 0;
}

.upload-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.upload-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.empty-hint {
  text-align: center;
  padding: 48px 0;
  color: #bbb;
  font-size: 14px;
}

.empty-icon-large {
  font-size: 56px;
  margin-bottom: 12px;
}

.empty-sub {
  font-size: 12px;
  color: #ccc;
  margin-top: 6px;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.media-card {
  border: 1px solid #eee;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.15s;
}

.media-card:hover {
  box-shadow: 0 4px 14px rgba(0,0,0,0.08);
}

.media-preview {
  width: 100%;
  height: 160px;
  background: #f5f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
}

.media-thumb {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.media-unknown {
  font-size: 48px;
  opacity: 0.5;
}

.media-info {
  padding: 8px 12px 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.media-name {
  font-size: 13px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-type-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #eef2ff;
  color: #667eea;
  flex-shrink: 0;
}

.media-time {
  font-size: 11px;
  color: #bbb;
  flex-shrink: 0;
}

.media-url-row {
  padding: 4px 12px;
  font-size: 11px;
  color: #aaa;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.media-actions {
  padding: 8px 12px 10px;
  display: flex;
  gap: 8px;
}

/* 使用场景 */
.scenario-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.scenario-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 10px;
  font-size: 13px;
  flex-wrap: wrap;
}

.scenario-step {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e24b4b;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.scenario-arrow {
  color: #ccc;
  font-size: 18px;
}

.scenario-card a {
  color: #e24b4b;
  font-weight: 600;
}
</style>
