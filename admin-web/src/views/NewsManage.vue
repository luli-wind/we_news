<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题" style="width: 240px" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="草稿" value="DRAFT" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openCreate">新建新闻</el-button>
    </div>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="publishedAt" label="发布时间" width="180" />
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

    <el-dialog v-model="showDialog" :title="form.id ? '编辑新闻' : '新建新闻'" width="720px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
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
import { fetchNews, createNews, updateNews, deleteNews } from '../api/modules'

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '' })
const total = ref(0)
const list = ref([])
const showDialog = ref(false)
const form = reactive({ id: null, title: '', summary: '', content: '', category: '', coverUrl: '', status: 'DRAFT' })

const load = async () => {
  const data = await fetchNews(query)
  list.value = data.list
  total.value = data.total
}

const onPageChange = (val) => {
  query.page = val
  load()
}

const openCreate = () => {
  Object.assign(form, { id: null, title: '', summary: '', content: '', category: '', coverUrl: '', status: 'DRAFT' })
  showDialog.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  showDialog.value = true
}

const save = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('标题和内容必填')
    return
  }
  if (form.id) {
    await updateNews(form.id, form)
  } else {
    await createNews(form)
  }
  ElMessage.success('保存成功')
  showDialog.value = false
  load()
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除该新闻?')
  await deleteNews(id)
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
