<template>
  <div class="admin-page">
    <div class="page-head">
      <div>
        <h1 class="page-title">评论管理</h1>
        <p class="page-subtitle">按内容类型与内容编号查询评论线程</p>
      </div>
    </div>

    <div class="card-surface">
      <div class="toolbar-row">
        <el-select v-model="query.bizType" style="width: 150px">
          <el-option label="新闻" value="NEWS" />
          <el-option label="视频" value="VIDEO" />
        </el-select>
        <el-input-number v-model="query.bizId" :min="1" controls-position="right" />
        <el-button type="primary" @click="load">查询评论</el-button>
      </div>

      <el-table :data="flatComments" border>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="userNickname" label="用户" width="160" />
        <el-table-column prop="content" label="评论内容" min-width="300" />
        <el-table-column prop="createdAt" label="创建时间" width="190" />
        <el-table-column label="操作" width="130">
          <template #default="scope">
            <el-button type="danger" size="small" @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchComments, deleteComment } from '../api/modules'

const query = reactive({ bizType: 'NEWS', bizId: 1 })
const comments = ref([])

const flatComments = computed(() => {
  const rows = []
  comments.value.forEach((item) => {
    rows.push(item)
    ;(item.replies || []).forEach((reply) => rows.push(reply))
  })
  return rows
})

const load = async () => {
  comments.value = await fetchComments(query)
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除该评论吗？')
  await deleteComment(id)
  ElMessage.success('删除成功')
  load()
}
</script>
