<template>
  <div class="tenant-view">
    <a-page-header title="租户管理" sub-title="管理团队信息与成员" />

    <!-- 租户信息 -->
    <a-descriptions bordered :column="2" class="tenant-info" size="middle">
      <a-descriptions-item label="团队名称">{{ tenantInfo?.name }}</a-descriptions-item>
      <a-descriptions-item label="标识">{{ tenantInfo?.slug }}</a-descriptions-item>
      <a-descriptions-item label="成员数">{{ tenantInfo?.memberCount }}</a-descriptions-item>
      <a-descriptions-item label="文档数">{{ tenantInfo?.documentCount }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ tenantInfo?.createdAt }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="tenantInfo?.status === 1 ? 'green' : 'red'">
          {{ tenantInfo?.status === 1 ? '正常' : '禁用' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="操作" :span="2">
        <a-button type="link" @click="showEditModal">编辑</a-button>
      </a-descriptions-item>
    </a-descriptions>

    <!-- 成员列表 -->
    <div class="member-section">
      <div class="member-header">
        <h3>成员列表</h3>
        <a-button type="primary" @click="showInviteModal">
          <template #icon><PlusOutlined /></template>
          邀请成员
        </a-button>
      </div>
      <a-table
        :columns="memberColumns"
        :data-source="memberList"
        :loading="memberLoading"
        :pagination="memberPagination"
        row-key="userId"
        @change="handleMemberTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-tag :color="roleColorMap[record.role] || 'default'">{{ record.role }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '活跃' : '禁用' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button
              v-if="record.role !== 'OWNER'"
              type="link"
              size="small"
              @click="showRoleModal(record)"
            >修改角色</a-button>
            <a-popconfirm
              v-if="record.role !== 'OWNER'"
              title="确定移除该成员？"
              @confirm="handleRemoveMember(record.userId)"
            >
              <a-button type="link" danger size="small">移除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 编辑租户弹窗 -->
    <a-modal v-model:open="editVisible" title="编辑租户" @ok="handleEditTenant">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="团队名称" :rules="[{ required: true, message: '请输入团队名称' }]">
          <a-input v-model:value="editForm.name" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 邀请成员弹窗 -->
    <a-modal v-model:open="inviteVisible" title="邀请成员" @ok="handleInvite">
      <a-form :model="inviteForm" layout="vertical">
        <a-form-item label="邮箱" :rules="[{ required: true, message: '请输入邮箱' }, { type: 'email', message: '邮箱格式不正确' }]">
          <a-input v-model:value="inviteForm.email" placeholder="请输入成员邮箱" />
        </a-form-item>
        <a-form-item label="角色" :rules="[{ required: true, message: '请选择角色' }]">
          <a-select v-model:value="inviteForm.role" placeholder="请选择角色">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="MEMBER">成员</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="初始密码" :rules="[{ required: true, message: '请输入初始密码' }, { min: 8, message: '密码至少8位' }, { pattern: /.*[A-Z].*/, message: '需包含大写字母' }, { pattern: /.*[a-z].*/, message: '需包含小写字母' }, { pattern: /.*\d.*/, message: '需包含数字' }]">
          <a-input-password v-model:value="inviteForm.password" placeholder="至少8位，含大小写字母和数字" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 修改角色弹窗 -->
    <a-modal v-model:open="roleVisible" title="修改角色" @ok="handleUpdateRole">
      <a-form layout="vertical">
        <a-form-item label="新角色">
          <a-select v-model:value="roleForm.role">
            <a-select-option value="ADMIN">管理员</a-select-option>
            <a-select-option value="MEMBER">成员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { TenantInfo, MemberInfo } from '@/types/api'
import {
  getCurrentTenant,
  updateCurrentTenant,
  listMembers,
  inviteMember,
  updateMemberRole,
  removeMember,
} from '@/api/tenant'

const tenantInfo = ref<TenantInfo | null>(null)
const memberList = ref<MemberInfo[]>([])
const memberLoading = ref(false)
const memberPagination = reactive({ current: 1, pageSize: 20, total: 0 })

const editVisible = ref(false)
const editForm = reactive({ name: '' })

const inviteVisible = ref(false)
const inviteForm = reactive({ email: '', role: 'MEMBER', password: '' })

const roleVisible = ref(false)
const roleForm = reactive({ userId: 0, role: '' })

const roleColorMap: Record<string, string> = {
  OWNER: 'gold',
  ADMIN: 'blue',
  MEMBER: 'default',
}

const memberColumns = [
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '昵称', dataIndex: 'displayName', key: 'displayName' },
  { title: '角色', key: 'role' },
  { title: '状态', key: 'status' },
  { title: '加入时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 180 },
]

async function fetchTenant() {
  try {
    const res = await getCurrentTenant()
    tenantInfo.value = res.data
  } catch { /* handled by interceptor */ }
}

async function fetchMembers() {
  memberLoading.value = true
  try {
    const res = await listMembers(memberPagination.current, memberPagination.pageSize)
    memberList.value = res.data.list
    memberPagination.total = res.data.total
  } catch { /* handled by interceptor */ } finally {
    memberLoading.value = false
  }
}

function handleMemberTableChange(pagination: any) {
  memberPagination.current = pagination.current
  memberPagination.pageSize = pagination.pageSize
  fetchMembers()
}

function showEditModal() {
  editForm.name = tenantInfo.value?.name || ''
  editVisible.value = true
}

async function handleEditTenant() {
  try {
    const res = await updateCurrentTenant({ name: editForm.name })
    tenantInfo.value = res.data
    editVisible.value = false
    message.success('更新成功')
  } catch { /* handled by interceptor */ }
}

function showInviteModal() {
  inviteForm.email = ''
  inviteForm.role = 'MEMBER'
  inviteForm.password = ''
  inviteVisible.value = true
}

async function handleInvite() {
  try {
    await inviteMember({ email: inviteForm.email, role: inviteForm.role, password: inviteForm.password })
    inviteVisible.value = false
    message.success('邀请成功')
    fetchMembers()
  } catch { /* handled by interceptor */ }
}

function showRoleModal(record: MemberInfo) {
  roleForm.userId = record.userId
  roleForm.role = record.role
  roleVisible.value = true
}

async function handleUpdateRole() {
  try {
    await updateMemberRole(roleForm.userId, roleForm.role)
    roleVisible.value = false
    message.success('角色已更新')
    fetchMembers()
  } catch { /* handled by interceptor */ }
}

async function handleRemoveMember(userId: number) {
  try {
    await removeMember(userId)
    message.success('已移除')
    fetchMembers()
  } catch { /* handled by interceptor */ }
}

onMounted(() => {
  fetchTenant()
  fetchMembers()
})
</script>

<style scoped>
.tenant-view {
  max-width: 960px;
}

.tenant-info {
  margin-bottom: 24px;
}

.member-section {
  margin-top: 16px;
}

.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.member-header h3 {
  margin: 0;
  font-size: 16px;
}
</style>
