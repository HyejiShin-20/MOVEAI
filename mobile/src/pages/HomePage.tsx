import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { DeliveryJobSummary, GuidanceSession } from '../api/models'
import { BottomNav } from '../components/BottomNav'
import { TopAppBar } from '../components/TopAppBar'
import { publicAsset } from '../publicAsset'
import { routes } from '../routes'
import { guidanceSessionStore } from '../state/guidanceSession'
import { reportDraftStore } from '../state/reportDraft'

const recentReports = [
  { status: '임시 저장', tone: 'draft', date: '어제', title: '△△상가 화물엘리베이터 위치', description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."' },
  { status: '업로드 실패', tone: 'failed', date: '7/13', title: 'XX빌라 공동현관 비밀번호 변경', description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."' },
  { status: '검수중', tone: 'review', date: '6/6', title: 'OO아파트 지하주차장 입구', description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."' },
] as const

export function HomePage() {
  const navigate = useNavigate()
  const [jobs, setJobs] = useState<DeliveryJobSummary[]>([])
  const [activeGuidance, setActiveGuidance] = useState<GuidanceSession | null>(null)
  const [jobsError, setJobsError] = useState('')

  useEffect(() => {
    let active = true
    api.deliveryJobs()
      .then((response) => { if (active) setJobs(response) })
      .catch(() => { if (active) setJobsError('배송 목록을 불러오지 못했습니다.') })

    const saved = guidanceSessionStore.read()
    if (saved) {
      api.guidance(saved.sessionId)
        .then((response) => { if (active) setActiveGuidance(response) })
        .catch(() => guidanceSessionStore.clear())
    }
    return () => { active = false }
  }, [])

  const defaultJob = useMemo(() => jobs.find((job) => job.jobCode === 'JOB_B_01') ?? jobs[0], [jobs])
  const openGuidance = (jobId?: number) => {
    const selectedId = jobId ?? defaultJob?.id
    navigate(selectedId ? `${routes.guidancePreview}?jobId=${selectedId}` : routes.guidancePreview)
  }

  return (
    <div className="mobile-page home-page">
      <section className="home-hero">
        <TopAppBar onMenu={() => navigate(routes.reportDrafts)} onProfile={() => navigate(routes.myReports)} />
        {activeGuidance && (
          <div className="ongoing-guidance">
            <div className="ongoing-guidance__copy">
              <strong>진행 중인 배송 안내가 있습니다</strong>
              <span>{activeGuidance.route.name} · {activeGuidance.currentStep.sequenceNo}/{activeGuidance.route.totalSteps}단계</span>
            </div>
            <button type="button" className="ongoing-guidance__end" onClick={() => navigate(`${routes.guidanceStep}?sessionId=${activeGuidance.sessionId}`)}>이어가기</button>
          </div>
        )}
      </section>

      <main className="home-content">
        <section className="primary-actions" aria-label="빠른 실행">
          <button type="button" className="primary-action primary-action--tip" onClick={() => { reportDraftStore.reset(); navigate(routes.reportRecord) }}>
            <img src={publicAsset('mic-white.svg')} alt="" /><span>현장 팁 기록</span>
          </button>
          <button type="button" className="primary-action primary-action--route" onClick={() => openGuidance()}>
            <img src={publicAsset('map.svg')} alt="" /><span>배송지 안내</span>
          </button>
        </section>

        <section className="home-section">
          <h2>배송 목록</h2>
          <div className="recent-location-list">
            {jobs.map((job) => (
              <button type="button" className="recent-location" key={job.id} onClick={() => openGuidance(job.id)}>
                <img className="recent-location__pin" src={publicAsset('location.svg')} alt="" />
                <span className="recent-location__copy"><strong>{job.placeName} · {job.recipientLabel}</strong><span>{job.addressText} · {job.itemSummary}</span></span>
                <img className="recent-location__chevron" src={publicAsset('chevron.svg')} alt="" />
              </button>
            ))}
            {!jobs.length && <p className="delivery-list-state">{jobsError || '배송 목록을 불러오는 중입니다.'}</p>}
          </div>
        </section>

        <section className="home-section home-section--reports">
          <h2>최근 제보</h2>
          <div className="report-list">
            {recentReports.map((report) => (
              <article className={`report-card report-card--${report.tone}`} key={report.title}>
                <div className="report-card__meta"><span className={`report-status report-status--${report.tone}`}>{report.status}</span><time>{report.date}</time></div>
                <strong className="report-card__title">{report.title}</strong><p>{report.description}</p>
              </article>
            ))}
          </div>
        </section>
      </main>
      <BottomNav />
    </div>
  )
}
