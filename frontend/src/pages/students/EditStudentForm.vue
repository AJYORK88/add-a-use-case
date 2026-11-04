<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:">
      <div class="status">
        <el-tag v-if="studentBeingEdited.enabled" type="success" data-cy="student-status"
          >Active</el-tag
        >
        <el-tag v-else type="info" data-cy="student-status">Deactivated</el-tag>
        <el-button
          v-if="studentBeingEdited.enabled"
          type="danger"
          plain
          size="small"
          :loading="deactivating"
          data-cy="student-deactivate"
          @click="confirmDeactivation()"
        >
          Deactivate
        </el-button>
      </div>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog', 'student-deactivated'])

const deactivating = ref(false)

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}

// UC-STU-deactivate-student steps 3 to 6. The consequences are the use case's Associated Information; the promise that
// she can be reactivated is left out until UC-STU-reactivate-student exists (OI-67).
async function confirmDeactivation() {
  const name = `${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}`
  try {
    await ElMessageBox.confirm(
      `${name} will no longer be able to log in or submit weekly activity reports and peer evaluations, ` +
        'and any session already open ends at its next action. The account and all submitted work ' +
        'are kept, and the student stays on the same team and course section. ' +
        'Deactivation is not deletion.',
      `Deactivate ${name}?`,
      { confirmButtonText: 'Deactivate', cancelButtonText: 'Cancel', type: 'warning' }
    )
  } catch {
    return // 4a: cancelled, nothing changes
  }

  deactivating.value = true
  try {
    const result = await deactivateStudent(studentBeingEdited.value.id as number)
    studentBeingEdited.value.enabled = result.data.enabled
    ElMessage.success(`${name} has been deactivated.`)
    emit('student-deactivated')
  } catch {
    // 3a: the shared handler has already shown "This student is already deactivated."; show her as she is now.
    // A 403 (3b) has already moved the user to the forbidden page, so reloading is harmless there too.
    await loadStudent(studentBeingEdited.value.id as number)
  } finally {
    deactivating.value = false
  }
}
</script>

<style scoped lang="scss">
.status {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
