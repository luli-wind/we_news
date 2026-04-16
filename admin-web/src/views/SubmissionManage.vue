<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 180px">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="mediaType" label="媒体类型" width="120" />
      <el-table-column prop="mediaUrl" label="媒体URL" />
      <el-table-column label="操作" width="260">
        <template #default="scope">
          <el-button size="small" type="success" @click="audit(scope.row.id, 'APPROVED')">通过</el-button>
          <el-button size="small" type="warning" @click="audit(scope.row.id, 'REJECTED')">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top: 12px"
      background
      layout="prev, pager, next, total"
      :total="total"
      :page-size="query.pageSize"
      :current-page="query.page"
      @current-change="onPageChange"
    />
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchSubmissions, auditSubmission } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, status: '' })
const total = ref(0)
const list = ref([])

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
  await auditSubmission(id, { status, reviewRemark: status === 'APPROVED' ? '审核通过' : '内容不符合要求' })
  ElMessage.success('审核完成')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
