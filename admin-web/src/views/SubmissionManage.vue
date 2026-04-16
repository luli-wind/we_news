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
        <el-select v-model="query.status" clearable placeholder="检查状态" style="width: 190px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="同意发布" value="APPROVED" />
          <el-option label="拒绝发布" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="load">查找</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mediaType" label="媒体类型" width="130" />
        <el-table-column prop="mediaUrl" label="媒体地址" min-width="240" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="scope">
            <el-button size="small" type="success" @click="audit(scope.row.id, 'APPROVED')">Approve</el-button>
            <el-button size="small" type="warning" @click="audit(scope.row.id, 'REJECTED')">Reject</el-button>
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
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSubmissions, auditSubmission } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, status: '' })
const total = ref(0)
const list = ref([])

const statusType = (status) => {
  if (status === 'APPROVED') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'warning'
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

const audit = async (id, status) => {
  await auditSubmission(id, {
    status,
    reviewRemark: status === 'APPROVED' ? 'Approved by admin panel' : 'Rejected by admin panel'
  })
  ElMessage.success('Review completed')
  load()
}

onMounted(load)
</script>