<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">投稿审核</h1>
        <p class="page-subtitle">审核用户生成的内容并安全发布</p>
      </div>
    </div>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 190px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button
          v-if="selectedIds.length > 0"
          type="success"
          @click="batchApprove"
        >批量通过 ({{ selectedIds.length }})</el-button>
        <el-button
          v-if="selectedIds.length > 0"
          type="danger"
          @click="batchRejectOpen"
        >批量驳回 ({{ selectedIds.length }})</el-button>
      </div>

      <el-table :data="list" border @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容摘要" min-width="200">
          <template #default="scope">
            <span class="content-preview">{{ truncate(scope.row.content, 80) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="mediaType" label="媒体类型" width="100" />
        <el-table-column prop="reviewRemark" label="审核备注" min-width="180">
          <template #default="scope">
            <span v-if="scope.row.reviewRemark" class="remark-text">{{ scope.row.reviewRemark }}</span>
            <span v-else class="no-remark">--</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              size="small"
              type="success"
              @click="approve(scope.row.id)"
            >通过</el-button>
            <el-button
              v-if="scope.row.status === 'PENDING'"
              size="small"
              type="danger"
              @click="openReject(scope.row.id)"
            >驳回</el-button>
            <span v-else class="done-text">已完成</span>
            <el-button size="small" @click="preview(scope.row)" style="margin-left:8px">预览</el-button>
            <el-popconfirm
              title="确定要删除该投稿吗？删除后不可恢复。"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="remove(scope.row.id)"
            >
              <template #reference>
                <el-button size="small" class="delete-btn">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.page"
        @current-change="onPageChange"
      />
    </div>

    <!-- 预览对话框 -->
    <el-dialog v-model="previewVisible" width="680px">
      <template #header>
        <div class="preview-header">
          <span class="preview-header-title">📋 投稿详情</span>
          <el-tag v-if="previewData" :type="statusType(previewData.status)" size="small">{{ statusLabel(previewData.status) }}</el-tag>
        </div>
      </template>
      <template v-if="previewData">
        <div class="preview-body">
          <h2 class="preview-title">{{ previewData.title }}</h2>
          <div class="preview-meta">
            <span>投稿时间：{{ previewData.createdAt }}</span>
          </div>
          <div class="preview-divider"></div>
          <div class="preview-content">{{ previewData.content || '（无正文内容）' }}</div>
          <template v-if="previewData.mediaUrl">
            <div class="preview-divider"></div>
            <div class="preview-media-label">📎 附件预览</div>
            <div class="preview-media-wrap">
              <img v-if="previewData.mediaType === 'IMAGE'" :src="previewData.mediaUrl" class="preview-media-img" />
              <video v-else-if="previewData.mediaType === 'VIDEO'" :src="previewData.mediaUrl" class="preview-media-video" controls />
              <a v-else :href="previewData.mediaUrl" target="_blank" class="preview-media-link">{{ previewData.mediaUrl }}</a>
            </div>
          </template>
          <template v-if="previewData.reviewRemark">
            <div class="preview-divider"></div>
            <div class="preview-remark-card">
              <div class="preview-remark-label">💬 审核备注</div>
              <div class="preview-remark-text">{{ previewData.reviewRemark }}</div>
            </div>
          </template>
        </div>
      </template>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog v-model="rejectVisible" title="驳回投稿" width="480px">
      <el-form :model="rejectForm" label-width="80px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reviewRemark"
            type="textarea"
            :rows="3"
            placeholder="请填写驳回原因（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="doReject" :disabled="!rejectForm.reviewRemark.trim()">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSubmissions, auditSubmission, deleteSubmission } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, status: '' })
const total = ref(0)
const list = ref([])

const rejectVisible = ref(false)
const rejectForm = reactive({ id: null, reviewRemark: '' })
const selectedIds = ref([])

const previewVisible = ref(false)
const previewData = ref(null)

const preview = (row) => {
  previewData.value = row
  previewVisible.value = true
}

const onSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

const batchApprove = async () => {
  try {
    await Promise.all(selectedIds.value.map(id => auditSubmission(id, { status: 'APPROVED', reviewRemark: '' })))
    ElMessage.success(`已通过 ${selectedIds.value.length} 条投稿`)
    selectedIds.value = []
    load()
  } catch (e) {
    ElMessage.error('批量操作部分失败')
  }
}

const batchRejectOpen = () => {
  rejectForm.id = null
  rejectForm.reviewRemark = ''
  rejectForm.batchIds = [...selectedIds.value]
  rejectVisible.value = true
}

const statusType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
}

const statusLabel = (status) => {
  if (status === 'PENDING') return '待审核'
  if (status === 'APPROVED') return '已通过'
  if (status === 'REJECTED') return '已驳回'
  return status
}

const truncate = (text, max) => {
  if (!text) return ''
  return text.length > max ? text.slice(0, max) + '...' : text
}

const load = async () => {
  const data = await fetchSubmissions(query)
  list.value = data.list
  total.value = data.total
}

const onPageChange = (val) => {
  query.page = val
  load()
}

const approve = async (id) => {
  await auditSubmission(id, {
    status: 'APPROVED',
    reviewRemark: ''
  })
  ElMessage.success('已通过该投稿')
  load()
}

const openReject = (id) => {
  rejectForm.id = id
  rejectForm.batchIds = []
  rejectForm.reviewRemark = ''
  rejectVisible.value = true
}

const doReject = async () => {
  if (!rejectForm.reviewRemark.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  const ids = rejectForm.batchIds.length > 0 ? rejectForm.batchIds : [rejectForm.id]
  try {
    await Promise.all(ids.map(id => auditSubmission(id, {
      status: 'REJECTED',
      reviewRemark: rejectForm.reviewRemark.trim()
    })))
    ElMessage.success(`已驳回 ${ids.length} 条投稿`)
  } catch (e) {
    ElMessage.error('驳回操作失败')
  }
  rejectVisible.value = false
  selectedIds.value = []
  load()
}

const remove = async (id) => {
  await deleteSubmission(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.content-preview {
  color: #666;
  font-size: 13px;
  line-height: 1.5;
}

.remark-text {
  color: #b91c1c;
  font-size: 13px;
}

.no-remark {
  color: #ccc;
}

.done-text {
  color: #999;
  font-size: 13px;
}

.delete-btn {
  color: #dc2626 !important;
  border: 1px solid #fecaca !important;
  background: #fef2f2 !important;
}

.delete-btn:hover {
  color: #fff !important;
  background: #dc2626 !important;
  border-color: #dc2626 !important;
}

/* 预览弹窗 */
.preview-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-header-title {
  font-size: 17px;
  font-weight: 700;
}

.preview-body {
  padding: 4px 0;
}

.preview-title {
  margin: 0;
  font-size: 22px;
  font-weight: 800;
  color: #1a1a2e;
  line-height: 1.45;
}

.preview-meta {
  margin-top: 10px;
  font-size: 13px;
  color: #999;
}

.preview-divider {
  height: 1px;
  background: #eef0f3;
  margin: 18px 0;
}

.preview-content {
  font-size: 15px;
  color: #374151;
  line-height: 2;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-media-label {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  margin-bottom: 10px;
}

.preview-media-wrap {
  border-radius: 12px;
  overflow: hidden;
  background: #f9fafb;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
}

.preview-media-img {
  max-width: 100%;
  max-height: 460px;
  display: block;
}

.preview-media-video {
  width: 100%;
  max-height: 400px;
  outline: none;
}

.preview-media-link {
  color: #2563eb;
  font-size: 13px;
  word-break: break-all;
  padding: 20px;
}

.preview-remark-card {
  background: #fef2f2;
  border-left: 4px solid #ef4444;
  border-radius: 0 10px 10px 0;
  padding: 14px 16px;
}

.preview-remark-label {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
  margin-bottom: 6px;
}

.preview-remark-text {
  font-size: 14px;
  color: #7f1d1d;
  line-height: 1.7;
}
</style>
