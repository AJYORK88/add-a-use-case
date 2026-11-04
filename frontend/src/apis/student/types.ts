export interface Student {
  id?: number // Optional field, for example, when creating a new student, the ID should not be provided
  username: string
  password?: string
  enabled?: boolean
  roles?: string
  firstName: string
  lastName: string
  email: string
  sectionId?: number
  sectionName?: string
  teamId?: number
  teamName?: string
}

/**
 * A member of the caller's team as a student caller receives it from GET /students/teams/{teamId}: a Student without
 * its account status, because a student is never shown another student's status (CO-ferpa). An instructor or course
 * admin receives the full Student, with enabled.
 */
export type Teammate = Omit<Student, 'enabled' | 'password'>

export interface PaginationParams {
  page: number
  size: number
}

export interface StudentSearchCriteria {
  firstName?: string
  lastName?: string
  email?: string
  sectionId?: number
  sectionName?: string
  teamId?: number
}

export interface SearchStudentByCriteriaResponse {
  flag: boolean
  code: number
  message: string
  data: {
    content: Student[]
    pageable: {
      sort: {
        sorted: boolean
        unsorted: boolean
        empty: boolean
      }
      pageNumber: number
      pageSize: number
      offset: number
      unpaged: boolean
      paged: boolean
    }
    totalElements: number
    totalPages: number
    last: boolean
    size: number
    number: number
    sort: {
      sorted: boolean
      unsorted: boolean
      empty: boolean
    }
    numberOfElements: number
    first: boolean
    empty: boolean
  }
}

export interface FindStudentByIdResponse {
  flag: boolean
  code: number
  message: string
  data: Student
}

export interface FindStudentByTeamIdResponse {
  flag: boolean
  code: number
  message: string
  data: (Student | Teammate)[]
}

export interface DeactivateStudentResponse {
  flag: boolean
  code: number
  message: string
  data: Student
}

export interface CreateStudentResponse {
  flag: boolean
  code: number
  message: string
  data: Student
}

export interface RegistrationParams {
  courseId: number
  sectionId?: number
  registrationToken: string
  role: string
}

export interface UpdateStudentResponse {
  flag: boolean
  code: number
  message: string
  data: Student
}

export interface DeleteStudentResponse {
  flag: boolean
  code: number
  message: string
  data: null
}
