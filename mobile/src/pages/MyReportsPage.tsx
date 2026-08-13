import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const photos = [
  'https://www.figma.com/api/mcp/asset/44f77015-5c04-490c-a897-4181e38a08f0.png',
  'https://www.figma.com/api/mcp/asset/88b74a61-5051-4bfc-b4c2-df6b744c3870.png',
  'https://www.figma.com/api/mcp/asset/a81310f5-85a6-4007-b2b8-5b671e73433e.png',
  'https://www.figma.com/api/mcp/asset/7933b075-0381-436f-bcaf-ee06af76f38e.png',
]
const backIcon = 'https://www.figma.com/api/mcp/asset/f1aacc23-0d62-4188-9c46-7ab4ffd23f95.svg'
const profileIcon = 'https://www.figma.com/api/mcp/asset/bc4c1af9-b1c2-49e5-8661-4cd4fb6c8377.svg'
const navDelivery = 'https://www.figma.com/api/mcp/asset/5c3a41e3-8568-42d4-ba82-300e81d88cc5.svg'
const navVoice = 'https://www.figma.com/api/mcp/asset/4f866fcd-d05e-44d0-87fc-7744f552716a.svg'
const navRoute = 'https://www.figma.com/api/mcp/asset/3c750189-8c10-4903-942a-67d3d1366f0d.svg'
const navProfile = 'https://www.figma.com/api/mcp/asset/5555eac4-4c8c-4406-8d63-876e4fe69745.svg'
const calendarIcon = 'https://www.figma.com/api/mcp/asset/79f3ad90-6d2f-4e92-bf0a-53524998e7a7.svg'

const reports = [
  { title: '강남역 4번 출구 하역장', detail: 'B2 화물용 엘리베이터 앞', date: '2023.10.24 14:30 등록', status: '검수 대기', tone: 'pending' },
  { title: '판교 테크원타워 지하 진입로', detail: '높이 제한 2.1m 수정 요청', date: '2023.10.22 09:15 등록', status: '승인 완료', tone: 'approved' },
  { title: '여의도 파크원 1번 게이트', detail: '임시 주차 구역 안내선 불량', date: '2023.10.20 16:45 등록', status: '반려됨', tone: 'rejected', reason: '사진의 초점이 맞지 않아 현장 상황을 파악하기 어렵습니다. 재촬영 후 다시 등록해 주시기 바랍니다.' },
  { title: '마포 래미안 3단지 무인택배함', detail: '비밀번호 입력 패드 고장 위치', date: '2023.10.18 11:20 등록', status: '승인 완료', tone: 'approved' },
] as const

export function MyReportsPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState('전체')
  const tabs = ['전체', '검수 대기', '승인', '반려']
  const filtered = reports.filter((report) => filter === '전체' || (filter === '검수 대기' && report.tone === 'pending') || (filter === '승인' && report.tone === 'approved') || (filter === '반려' && report.tone === 'rejected'))
  return (
    <div className="mobile-page my-reports-page">
      <header className="my-reports-header"><div><button type="button" onClick={() => navigate('/home')}><img src={backIcon} alt="" /></button><strong>내 제보 내역</strong></div><button type="button"><img src={profileIcon} alt="" /></button></header>
      <main className="my-reports-main">
        <div className="report-tabs">{tabs.map((tab) => <button type="button" key={tab} className={filter === tab ? 'active' : ''} onClick={() => setFilter(tab)}>{tab}</button>)}</div>
        <div className="my-report-list">
          {filtered.map((report, index) => (
            <article className="my-report-card" key={report.title}>
              <div className="my-report-card__image"><img src={photos[reports.indexOf(report)]} alt="" /><span className={`history-status history-status--${report.tone}`}>{report.status}</span></div>
              <div className="my-report-card__body">
                <h2>{report.title}</h2><p>{report.detail}</p>
                {report.reason && <div className="rejection-note">▣ {report.reason}</div>}
                <div className="my-report-card__date"><img src={calendarIcon} alt="" />{report.date}{report.tone === 'rejected' && <button type="button" onClick={() => navigate('/reports/record')}>다시 등록</button>}</div>
              </div>
            </article>
          ))}
        </div>
      </main>
      <nav className="history-nav">
        <button onClick={() => navigate('/home')}><img src={navDelivery} alt="" /><span>Deliveries</span></button>
        <button className="active"><img src={navVoice} alt="" /><span>Voice Tip</span></button>
        <button><img src={navRoute} alt="" /><span>Navigation</span></button>
        <button><img src={navProfile} alt="" /><span>Profile</span></button>
      </nav>
    </div>
  )
}
