<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">操作日志</h1>
        <p class="page-subtitle">在管理模块中跟踪关键操作</p>
      </div>
    </div>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-button type="primary" @click="load">刷新</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="moduleName" label="模块" width="140" />
        <el-table-column prop="actionName" label="操作" width="150" />
        <el-table-column prop="operatorId" label="操作人员" width="100" />
        <el-table-column prop="detail" label="细节" min-width="260" />
        <el-table-column prop="createdAt" label="时间" width="190" />
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
import { fetchOperationLogs } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10 })
const total = ref(0)
const list = ref([])

const load = async () => {
  const data = await fetchOperationLogs(query)
  list.value = data.list
  total.value = data.total
}

const onPageChange = (val) => {
  query.page = val
  load()
}

onMounted(load)
</script>