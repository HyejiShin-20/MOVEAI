import type { ReportDraftState } from '../state/reportDraft'

export type ReportSubmissionResult = {
  reportId: string
  status: 'PENDING_REVIEW'
  submittedAt: string
}

/**
 * UI가 특정 HTTP 엔드포인트에 직접 의존하지 않도록 둔 프론트엔드 경계입니다.
 * 백엔드 API 계약이 확정되면 동일 인터페이스를 구현하는 HTTP repository로 교체합니다.
 */
export interface ReportRepository {
  saveDraft(draft: ReportDraftState): Promise<void>
  submit(draft: ReportDraftState): Promise<ReportSubmissionResult>
}

class BrowserReportRepository implements ReportRepository {
  async saveDraft(_draft: ReportDraftState) {
    // 현재는 reportDraftStore가 localStorage persistence를 담당한다.
  }

  async submit(_draft: ReportDraftState): Promise<ReportSubmissionResult> {
    return {
      reportId: `LOCAL-${Date.now()}`,
      status: 'PENDING_REVIEW',
      submittedAt: new Date().toISOString(),
    }
  }
}

export const reportRepository: ReportRepository = new BrowserReportRepository()
