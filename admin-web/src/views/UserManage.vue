<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column label="角色分配" width="280">
        <template #default="scope">
          <el-select v-model="roleMap[scope.row.id]" placeholder="选择角色" style="width: 140px">
            <el-option v-for="role in roles" :key="role.id" :label="role.code" :value="role.id" />
          </el-select>
          <el-button size="small" type="success" @click="assign(scope.row.id)">分配</el-button>
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
import { fetchUsers, fetchRoles, assignUserRole } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10 })
const total = ref(0)
const list = ref([])
const roles = ref([])
const roleMap = reactive({})

const load = async () => {
  const [userData, roleData] = await Promise.all([
    fetchUsers(query),
    fetchRoles()
  ])
  list.value = userData.list
  total.value = userData.total
  roles.value = roleData
}

const onPageChange = (val) => {
  query.page = val
  load()
}

const assign = async (userId) => {
  const roleId = roleMap[userId]
  if (!roleId) {
    ElMessage.warning('请先选择角色')
    return
  }
  await assignUserRole({ userId, roleId })
  ElMessage.success('分配成功')
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}
</style>
