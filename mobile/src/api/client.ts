import type {
  ApiErrorBody,
  DeliveryJobDetail,
  DeliveryJobSummary,
  GuidanceSession,
  VehicleInput,
} from './models'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export class ApiError extends Error {
  readonly code: string
  readonly status: number

  constructor(status: number, body: ApiErrorBody) {
    super(body.error.message)
    this.name = 'ApiError'
    this.code = body.error.code
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const fallback: ApiErrorBody = {
      error: { code: 'UNKNOWN_ERROR', message: '요청을 처리하지 못했습니다.' },
    }
    let body = fallback
    try {
      body = (await response.json()) as ApiErrorBody
    } catch {
      // 서버가 계약된 JSON 오류를 반환하지 못한 경우 fallback을 사용한다.
    }
    throw new ApiError(response.status, body)
  }

  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export const api = {
  deliveryJobs(status?: string) {
    const query = status ? `?status=${encodeURIComponent(status)}` : ''
    return request<DeliveryJobSummary[]>(`/api/delivery-jobs${query}`)
  },
  deliveryJob(id: number) {
    return request<DeliveryJobDetail>(`/api/delivery-jobs/${id}`)
  },
  startGuidance(deliveryJobId: number, vehicle: VehicleInput, contextTime?: string) {
    return request<GuidanceSession>('/api/guidance', {
      method: 'POST',
      body: JSON.stringify({ deliveryJobId, vehicle, ...(contextTime ? { contextTime } : {}) }),
    })
  },
  guidance(sessionId: number) {
    return request<GuidanceSession>(`/api/guidance/${sessionId}`)
  },
  nextGuidance(sessionId: number) {
    return request<GuidanceSession>(`/api/guidance/${sessionId}/next`, { method: 'POST' })
  },
  completeGuidance(sessionId: number) {
    return request<{ sessionId: number; status: 'COMPLETED'; completedAt: string }>(
      `/api/guidance/${sessionId}/complete`,
      { method: 'POST' },
    )
  },
}
