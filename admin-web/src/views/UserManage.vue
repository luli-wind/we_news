<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">用户与角色</h1>
        <p class="page-subtitle">为后台账户分配权限角色</p>
      </div>
    </div>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-button type="primary" @click="load">Refresh</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="180" />
        <el-table-column prop="nickname" label="用户名" min-width="140" />
        <el-table-column label="角色分配" width="300">
          <template #default="scope">
            <el-select v-model="roleMap[scope.row.id]" placeholder="选择角色" style="width: 150px">
              <el-option v-for="role in roles" :key="role.id" :label="role.code" :value="role.id" />
            </el-select>
            <el-button size="small" type="success" @click="assign(scope.row.id)">分配</el-button>
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
    ElMessage.warning('Please choose a role first')
    return
  }
  await assignUserRole({ userId, roleId })
  ElMessage.success('Role assigned')
}

onMounted(load)
</script>