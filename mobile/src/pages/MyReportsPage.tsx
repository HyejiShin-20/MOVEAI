import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { routes } from '../routes'

const photos = [
  'https://www.figma.com/api/mcp/asset/0d708c47-c214-4dd0-a1d9-cb69d7d2302d.png',
  'https://www.figma.com/api/mcp/asset/2021d73b-bbbf-435b-a8b2-303d9da102c1.png',
  'https://www.figma.com/api/mcp/asset/996fda91-ee79-4d9a-9aea-d78d0c3f3169.png',
  'https://www.figma.com/api/mcp/asset/17b888e2-258a-4dd1-a45c-e01150a253b9.png',
]

const navDelivery = 'https://www.figma.com/api/mcp/asset/286aa50d-eb8e-448d-82ca-99ce104c894e.svg'
const navVoice = 'https://www.figma.com/api/mcp/asset/543b92b3-94a3-4677-85c7-005bb5e35166.svg'
const navRoute = 'https://www.figma.com/api/mcp/asset/3ddc0b5a-f482-4966-9ad4-c0ee10fe2ff0.svg'
const navProfile = 'https://www.figma.com/api/mcp/asset/d49203eb-81f9-4656-a2ea-8e8095712905.svg'
const backIcon = 'https://www.figma.com/api/mcp/asset/75d3bd50-962e-4d44-a5c3-52ad4148d897.svg'
const profileIcon = 'https://www.figma.com/api/mcp/asset/8485e81b-b257-4ad0-a160-3ce1591678f5.svg'
const pendingIcon = 'https://www.figma.com/api/mcp/asset/0e59c78d-ca28-49d6-8768-a1a40e17d8d8.svg'
const calendarIcon = 'https://www.figma.com/api/mcp/asset/7ebc209c-a062-4eae-b6da-15262f092a10.svg'
const approvedIcon = 'https://www.figma.com/api/mcp/asset/84fe03a0-84be-4ced-b958-c9167c543ca7.svg'
const rejectedIcon = 'https://www.figma.com/api/mcp/asset/4bb8a536-8bc2-450f-8b78-2c83b3c490e0.svg'
const rejectionIcon = 'https://www.figma.com/api/mcp/asset/f5f5c9ad-9498-48a0-b00f-ce422c7439e4.svg'

type ReportTone = 'pending' | 'approved' | 'rejected'

type Report = {
  title: string
  detail: string
  date: string
  status: string
  tone: ReportTone
  reason?: string
}

const reports: Report[] = [
  { title: '강남역 4번 출구 하역장', detail: 'B2 화물용 엘리베이터 앞', date: '2023.10.24 14:30 등록', status: '검수 대기', tone: 'pending' },
  { title: '판교 테크원타워 지하 진입로', detail: '높이 제한 2.1m 수정 요청', date: '2023.10.22 09:15 등록', status: '승인 완료', tone: 'approved' },
  { title: '여의도 파크원 1번 게이트', detail: '임시 주차 구역 안내선 불량', date: '2023.10.20 16:45 등록', status: '반려됨', tone: 'rejected', reason: '사진의 초점이 맞지 않아 현장 상황을 파악하기 어렵습니다. 재촬영 후 다시 등록해 주시기 바랍니다.' },
  { title: '마포 래미안 3단지 무인택배함', detail: '비밀번호 입력 패드 고장 위치', date: '2023.10.18 11:20 등록', status: '승인 완료', tone: 'approved' },
]

const statusIcons: Record<ReportTone, string> = {
  pending: pendingIcon,
  approved: approvedIcon,
  rejected: rejectedIcon,
}

export function MyReportsPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState('전체')
  const tabs = ['전체', '검수 대기', '승인', '반려']
  const filtered = reports.filter((report) =>
    filter === '전체' ||
    (filter === '검수 대기' && report.tone === 'pending') ||
    (filter === '승인' && report.tone === 'approved') ||
    (filter === '반려' && report.tone === 'rejected'),
  )

  return (
    <div className="mobile-page my-reports-page" data-figma-node="118:9327">
      <header className="my-reports-header">
        <div>
          <button type="button" aria-label="뒤로가기" onClick={() => navigate(routes.home)}><img src={backIcon} alt="" /></button>
          <strong>내 제보 내역</strong>
        </div>
        <button type="button" aria-label="프로필"><img src={profileIcon} alt="" /></button>
      </header>

      <main className="my-reports-main">
        <div className="report-tabs" role="tablist" aria-label="제보 상태 필터">
          {tabs.map((tab) => (
            <button
              type="button"
              role="tab"
              aria-selected={filter === tab}
              key={tab}
              className={filter === tab ? 'active' : ''}
              onClick={() => setFilter(tab)}
            >
              {tab}
            </button>
          ))}
        </div>

        <div className="my-report-list">
          {filtered.map((report) => {
            const originalIndex = reports.indexOf(report)
            return (
              <article className="my-report-card" key={report.title}>
                <div className="my-report-card__image">
                  <img src={photos[originalIndex]} alt="" />
                  <span className={`history-status history-status--${report.tone}`}>
                    <img src={statusIcons[report.tone]} alt="" />{report.status}
                  </span>
                </div>

                <div className="my-report-card__body">
                  <h2>{report.title}</h2>
                  <p>{report.detail}</p>

                  {report.reason && (
                    <div className="rejection-note">
                      <img src={rejectionIcon} alt="" />
                      <span>{report.reason}</span>
                    </div>
                  )}

                  <div className="my-report-card__date">
                    <img src={calendarIcon} alt="" />
                    <span>{report.date}</span>
                    {report.tone === 'rejected' && (
<<<<<<< HEAD
                      <button type="button" onClick={() => navigate('/reports/record')}>다시 등록</button>
=======
                      <button type="button" onClick={() => navigate(routes.reportRecord)}>다시 등록</button>
>>>>>>> feature/front
                    )}
                  </div>
                </div>
              </article>
            )
          })}
        </div>
      </main>

      <nav className="history-nav" aria-label="하단 메뉴">
<<<<<<< HEAD
        <button type="button" onClick={() => navigate('/home')}><img src={navDelivery} alt="" /><span>Deliveries</span></button>
        <button type="button" className="active"><img src={navVoice} alt="" /><span>Voice Tip</span></button>
        <button type="button" onClick={() => navigate('/guidance/preview')}><img src={navRoute} alt="" /><span>Navigation</span></button>
        <button type="button"><img src={navProfile} alt="" /><span>Profile</span></button>
=======
        <button type="button" onClick={() => navigate(routes.home)}><img src={navDelivery} alt="" /><span>Deliveries</span></button>
        <button type="button" onClick={() => navigate(routes.reportRecord)}><img src={navVoice} alt="" /><span>Voice Tip</span></button>
        <button type="button" onClick={() => navigate(routes.guidancePreview)}><img src={navRoute} alt="" /><span>Navigation</span></button>
        <button type="button" className="active" aria-current="page"><img src={navProfile} alt="" /><span>Profile</span></button>
>>>>>>> feature/front
      </nav>
    </div>
  )
}
