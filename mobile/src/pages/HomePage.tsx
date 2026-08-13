import { useNavigate } from 'react-router-dom'
import { BottomNav } from '../components/BottomNav'
import { TopAppBar } from '../components/TopAppBar'

const recentLocations = [
  { title: '송파구 올림픽로 300', subtitle: '롯데월드타워 하역장 B2' },
  { title: '송파구 올림픽로 300', subtitle: '롯데월드타워 하역장 B2' },
]

const recentReports = [
  {
    status: '임시 저장',
    tone: 'draft',
    date: '어제',
    title: '△△상가 화물엘리베이터 위치',
    description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."',
  },
  {
    status: '업로드 실패',
    tone: 'failed',
    date: '7/13',
    title: 'XX빌라 공동현관 비밀번호 변경',
    description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."',
  },
  {
    status: '검수중',
    tone: 'review',
    date: '6/6',
    title: 'OO아파트 지하주차장 입구',
    description: '"입구 경사로가 가팔라서 1톤 탑차 진입 시 주의 필요..."',
  },
] as const

export function HomePage() {
  const navigate = useNavigate()

  return (
    <div className="mobile-page home-page">
      <section className="home-hero">
        <TopAppBar />
        <div className="ongoing-guidance">
          <div className="ongoing-guidance__copy">
            <strong>진행 중인 배송 안내가 있습니다</strong>
            <span>OO아파트 101동. 현재 단계: 101동 출입구</span>
          </div>
          <button type="button" className="ongoing-guidance__end">종료</button>
        </div>
      </section>

      <main className="home-content">
        <section className="primary-actions" aria-label="빠른 실행">
          <button type="button" className="primary-action primary-action--tip" onClick={() => navigate('/reports/record')}>
            <img src="/assets/mic-white.svg" alt="" />
            <span>현장 팁 기록</span>
          </button>
          <button type="button" className="primary-action primary-action--route" onClick={() => navigate('/guidance/preview')}>
            <img src="/assets/map.svg" alt="" />
            <span>배송지 안내</span>
          </button>
        </section>

        <section className="home-section">
          <h2>최근 검색 장소</h2>
          <div className="recent-location-list">
            {recentLocations.map((location, index) => (
              <button type="button" className="recent-location" key={`${location.title}-${index}`}>
                <img className="recent-location__pin" src="/assets/location.svg" alt="" />
                <span className="recent-location__copy">
                  <strong>{location.title}</strong>
                  <span>{location.subtitle}</span>
                </span>
                <img className="recent-location__chevron" src="/assets/chevron.svg" alt="" />
              </button>
            ))}
          </div>
        </section>

        <section className="home-section home-section--reports">
          <h2>최근 제보</h2>
          <div className="report-list">
            {recentReports.map((report) => (
              <article className={`report-card report-card--${report.tone}`} key={report.title}>
                <div className="report-card__meta">
                  <span className={`report-status report-status--${report.tone}`}>{report.status}</span>
                  <time>{report.date}</time>
                </div>
                <strong className="report-card__title">{report.title}</strong>
                <p>{report.description}</p>
              </article>
            ))}
          </div>
        </section>
      </main>

      <BottomNav />
    </div>
  )
}
