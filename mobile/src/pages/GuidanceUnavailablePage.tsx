import { useLocation, useNavigate } from 'react-router-dom'
import { routes } from '../routes'

const menuIcon = 'https://www.figma.com/api/mcp/asset/9e73c706-0690-490c-b392-e01406f69332.svg'
const infoIcon = 'https://www.figma.com/api/mcp/asset/b3de321a-60a8-447a-9623-284123e81b39.svg'
const vehicleIcon = 'https://www.figma.com/api/mcp/asset/60650522-8705-4259-9b36-bbe7f111ff3e.svg'
const placeIcon = 'https://www.figma.com/api/mcp/asset/42c7b6c7-e31a-42e4-9d94-7f70485bb227.svg'

export function GuidanceUnavailablePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as {
    jobId?: number
    message?: string
    tonnage?: string
    heightM?: string
    widthM?: string
  } | null
  const retryPath = state?.jobId ? `${routes.guidancePreview}?jobId=${state.jobId}` : routes.guidancePreview

  return (
    <div className="mobile-page unavailable-page" data-figma-node="118:8769">
      <header className="guidance-header guidance-header--plain">
        <button type="button" aria-label="이전 화면" onClick={() => navigate(routes.home)}>
          <img src={menuIcon} alt="" />
        </button>
      </header>

      <main className="unavailable-main">
        <section className="unavailable-visual">
          <div aria-hidden="true" />
          <h1>경로 정보가 없습니다</h1>
          <p>
            검색하신 장소에 등록된 Last 100m 경로가 없거나,<br />
            {state?.message ?? '현재 차량 조건과 맞는 경로를 찾을 수 없습니다.'}
          </p>
        </section>

        <section className="constraint-box">
          <img src={infoIcon} alt="" />
          <div>
            <h2>차량 진입 제약 안내</h2>
            <p>
              입력한 차량: {state?.tonnage ?? '-'}t · 높이 {state?.heightM ?? '-'}m
              {state?.widthM ? ` · 너비 ${state.widthM}m` : ''}. 등록된 경로의 차량 조건을 다시 확인해 주세요.
            </p>
          </div>
        </section>

        <section className="unavailable-actions">
          <button className="primary" type="button" onClick={() => navigate(retryPath)}>
            <img src={vehicleIcon} alt="" />
            <span>차량 조건 다시 입력</span>
          </button>
          <button className="secondary" type="button" onClick={() => navigate(routes.reportPlace)}>
            <img src={placeIcon} alt="" />
            <span>장소 정보 보기</span>
          </button>
          <button className="tertiary" type="button" onClick={() => navigate(routes.home)}>안내 종료</button>
        </section>
      </main>
    </div>
  )
}
