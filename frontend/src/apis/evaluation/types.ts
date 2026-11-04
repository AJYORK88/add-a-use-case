export interface PeerEvaluation {
  evaluationId?: number
  week: string
  evaluatorId?: number
  evaluatorName?: string
  evaluateeId: number
  evaluateeName?: string
  ratings: Rating[]
  totalScore?: number
  publicComment?: string
  privateComment?: string
  createdAt?: string // In the format "MM-dd-yyyy HH:mm:ss"
  updatedAt?: string // In the format "MM-dd-yyyy HH:mm:ss"
}

export interface Rating {
  ratingId?: number
  criterionId: number
  criterion: string
  actualScore: number
}

export interface FetchPeerEvaluationsResponse {
  flag: boolean
  code: number
  message: string
  data: PeerEvaluation[]
}

export interface SubmitPeerEvaluationResponse {
  flag: boolean
  code: number
  message: string
  data: PeerEvaluation
}

export interface UpdatePeerEvaluationResponse {
  flag: boolean
  code: number
  message: string
  data: PeerEvaluation
}

export interface PeerEvaluationAverage {
  studentId: number
  week: string
  firstName: string
  lastName: string
  email: string
  teamName: string | null // Null for a student who is on no team: she may never have been assigned one, or have been removed from one
  deactivated: boolean // Her account status now (UC-STU-deactivate-student); shown only on instructor pages
  averageTotalScore: number
  publicComments: string[]
  privateComments: string[]
  ratingAverages: RatingAverage[]
}

export interface RatingAverage {
  criterionId: number
  averageScore: number
}

export interface GeneratePeerEvaluationAveragesForOneStudentResponse {
  flag: boolean
  code: number
  message: string
  data: PeerEvaluationAverage[]
}

export interface PeriodParams {
  startWeek: string
  endWeek: string
}

export interface WeeklyPeerEvaluationReportForOneSection {
  sectionName: string
  week: string
  peerEvaluationAverages: PeerEvaluationAverage[]
}

export interface GenerateWeeklyPeerEvaluationAveragesForSectionResponse {
  flag: boolean
  code: number
  message: string
  data: WeeklyPeerEvaluationReportForOneSection
}

export interface GetWeeklyPeerEvaluationsForStudentResponse {
  flag: boolean
  code: number
  message: string
  data: PeerEvaluation[]
}

// A teammate the caller evaluates for a week: names only, never an account status (CO-ferpa)
export interface Evaluatee {
  id: number
  firstName: string
  lastName: string
}

export interface FindEvaluateesResponse {
  flag: boolean
  code: number
  message: string
  data: Evaluatee[]
}
