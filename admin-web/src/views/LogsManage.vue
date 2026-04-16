<template>
  <div>
    <el-button type="primary" @click="load">刷新日志</el-button>

    <el-table :data="list" border style="margin-top: 12px">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="moduleName" label="模块" width="140" />
      <el-table-column prop="actionName" label="动作" width="140" />
      <el-table-column prop="operatorId" label="操作人" width="100" />
      <el-table-column prop="detail" label="详情" />
      <el-table-column prop="createdAt" label="时间" width="180" />
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
