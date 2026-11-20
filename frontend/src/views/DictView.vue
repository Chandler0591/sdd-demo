<template>
  <div>
    <el-card>
      <template #header>
        <div class="header-row">
          <span>系统字典（分类等业务下拉选项）</span>
          <div>
            <el-button size="small" @click="load">刷新</el-button>
            <el-button size="small" type="success" @click="openCreate(null)">新增分类</el-button>
          </div>
        </div>
      </template>
      <el-table
        :data="tree"
        v-loading="loading"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
      >
        <el-table-column prop="code" label="编码" width="180" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreate(row)">新增子项</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑字典项' : '新增字典项'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" :disabled="!!editingId" clearable placeholder="创建后只读" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" clearable />
        </el-form-item>
        <el-form-item label="父级">
          <el-input :model-value="parentName" disabled placeholder="顶层（无父级）" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'
import type { DictVO } from '../types'

const tree = ref<DictVO[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref('')
const parent = ref<DictVO | null>(null)
const form = reactive({ code: '', name: '', pcode: '', sortOrder: 0 })

const parentName = computed(() => {
  if (!form.pcode) return '顶层（无父级）'
  return parent.value && parent.value.code === form.pcode
    ? `${parent.value.name}（${parent.value.code}）`
    : form.pcode
})

async function load() {
  loading.value = true
  try {
    const { data } = await http.get<DictVO[]>('/dicts')
    tree.value = data
  } finally {
    loading.value = false
  }
}

/** 新增：parent 非空时作为父项（新增子项），否则为顶层 */
function openCreate(parentItem: DictVO | null) {
  editingId.value = ''
  parent.value = parentItem
  Object.assign(form, { code: '', name: '', pcode: parentItem?.code ?? '', sortOrder: 0 })
  dialogVisible.value = true
}

function openEdit(row: DictVO) {
  editingId.value = row.id
  parent.value = null
  Object.assign(form, { code: row.code, name: row.name, pcode: row.pcode ?? '', sortOrder: row.sortOrder })
  dialogVisible.value = true
}

async function submit() {
  const payload = {
    code: form.code.trim(),
    name: form.name,
    pcode: form.pcode,
    sortOrder: form.sortOrder
  }
  if (editingId.value) {
    // 编辑仅改名称 / 排序，父级保持原值（code 创建后只读）
    await http.put(`/dicts/${editingId.value}`, { name: payload.name, pcode: payload.pcode, sortOrder: payload.sortOrder })
    ElMessage.success('修改成功')
  } else {
    await http.post('/dicts', payload)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
