import type { ReportDraftState } from '../state/reportDraft'
import { api } from './client'

export type ReportSubmissionResult = {
  reportId: string
  status: 'PENDING_REVIEW'
  submittedAt: string
  draftCount: number
}

export interface ReportRepository {
  saveDraft(draft: ReportDraftState): Promise<void>
  submit(draft: ReportDraftState): Promise<ReportSubmissionResult>
}

class HttpReportRepository implements ReportRepository {
  async saveDraft(_draft: ReportDraftState) {
    // 작성 중 상태는 reportDraftStore가 브라우저에 보존한다.
  }

  async submit(draft: ReportDraftState): Promise<ReportSubmissionResult> {
    if (!draft.selectedPlace) throw new Error('제보 장소를 선택해 주세요.')
    if (!draft.transcript.trim()) throw new Error('제보 내용을 입력해 주세요.')

    const created = await api.createTextReport(
      draft.selectedPlace.id,
      draft.selectedScopeNodeId,
      draft.transcript.trim(),
    )
    const extracted = await api.extractReport(created.reportId)
    if (extracted.status !== 'EXTRACTED') {
      throw new Error(extracted.reason || 'AI 지식 추출에 실패했습니다. 다시 시도해 주세요.')
    }
    return {
      reportId: String(created.reportId),
      status: 'PENDING_REVIEW',
      submittedAt: new Date().toISOString(),
      draftCount: extracted.drafts?.length ?? 0,
    }
  }
}

export const reportRepository: ReportRepository = new HttpReportRepository()
