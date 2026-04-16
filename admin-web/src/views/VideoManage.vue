<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题" style="width: 240px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新建视频</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="url" label="视频URL" />
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(scope.row.id)">删除</el-button>
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

    <el-dialog v-model="showDialog" :title="form.id ? '编辑视频' : '新建视频'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="视频URL"><el-input v-model="form.url" /></el-form-item>
        <el-form-item label="封面URL"><el-input v-model="form.coverUrl" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 160px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchVideos, createVideo, updateVideo, deleteVideo } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const total = ref(0)
const list = ref([])
const showDialog = ref(false)
const form = reactive({ id: null, title: '', description: '', url: '', coverUrl: '', status: 'DRAFT' })

const load = async () => {
  const data = await fetchVideos(query)
  list.value = data.list
  total.value = data.total
}

const onPageChange = (val) => {
  query.page = val
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, title: '', description: '', url: '', coverUrl: '', status: 'DRAFT' })
  showDialog.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  showDialog.value = true
}

const save = async () => {
  if (!form.title || !form.url) {
    ElMessage.warning('标题和视频URL必填')
    return
  }
  if (form.id) {
    await updateVideo(form.id, form)
  } else {
    await createVideo(form)
  }
  ElMessage.success('保存成功')
  showDialog.value = false
  load()
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除该视频?')
  await deleteVideo(id)
  ElMessage.success('删除成功')
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
