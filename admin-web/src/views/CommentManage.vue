<template>
  <div>
    <div class="toolbar">
      <el-select v-model="query.bizType" style="width: 140px">
        <el-option label="NEWS" value="NEWS" />
        <el-option label="VIDEO" value="VIDEO" />
      </el-select>
      <el-input-number v-model="query.bizId" :min="1" controls-position="right" />
      <el-button type="primary" @click="load">查询评论</el-button>
    </div>

    <el-table :data="flatComments" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userNickname" label="用户" width="140" />
      <el-table-column prop="content" label="内容" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button type="danger" size="small" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
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
  await ElMessageBox.confirm('确认删除该评论?')
  await deleteComment(id)
  ElMessage.success('删除成功')
  load()
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
