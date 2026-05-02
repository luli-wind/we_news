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
      </div>

      <el-table :data="list" border>
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
        <el-table-column label="操作" width="220" fixed="right">
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
import { fetchSubmissions, auditSubmission } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, status: '' })
const total = ref(0)
const list = ref([])

const rejectVisible = ref(false)
const rejectForm = reactive({ id: null, reviewRemark: '' })

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
  rejectForm.reviewRemark = ''
  rejectVisible.value = true
}

const doReject = async () => {
  if (!rejectForm.reviewRemark.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  await auditSubmission(rejectForm.id, {
    status: 'REJECTED',
    reviewRemark: rejectForm.reviewRemark.trim()
  })
  ElMessage.success('已驳回该投稿')
  rejectVisible.value = false
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
</style>
