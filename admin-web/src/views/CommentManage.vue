<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">评论管理</h1>
        <p class="page-subtitle">查看和管理所有用户评论</p>
      </div>
    </div>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-select v-model="query.bizType" placeholder="全部类型" clearable style="width: 140px" @change="search">
          <el-option label="全部" value="" />
          <el-option label="新闻" value="NEWS" />
          <el-option label="视频" value="VIDEO" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <span class="item-count">共 {{ total }} 条评论</span>
      </div>

      <el-table :data="list" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userNickname" label="用户" width="130" />
        <el-table-column prop="bizTitle" label="所属内容" min-width="200">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.bizType === 'NEWS' ? 'success' : 'warning'" style="margin-right:6px">
              {{ scope.row.bizType === 'NEWS' ? '新闻' : '视频' }}
            </el-tag>
            <span>{{ scope.row.bizTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="260">
          <template #default="scope">
            <span :class="{ 'reply-prefix': scope.row.parentId }">{{ scope.row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button type="danger" size="small" @click="remove(scope.row.id)">删除</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAllComments, deleteComment } from '../api/modules'

const query = reactive({ page: 1, pageSize: 20, bizType: '' })
const total = ref(0)
const list = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const data = await fetchAllComments(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.page = 1
  load()
}

const onPageChange = (val) => {
  query.page = val
  load()
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除该评论吗？')
  await deleteComment(id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.item-count {
  color: #999;
  font-size: 13px;
  margin-left: auto;
}

.reply-prefix {
  color: #888;
  font-size: 13px;
}
</style>
