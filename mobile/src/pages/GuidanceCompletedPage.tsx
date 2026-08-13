import { useNavigate } from 'react-router-dom'

const successIcon = 'https://www.figma.com/api/mcp/asset/a3a3dc63-3336-460e-8a09-a6f0291abed4.svg'
const gateIcon = 'https://www.figma.com/api/mcp/asset/cccd73f7-da68-49e0-8f34-9666b56ad9c3.svg'
const routeLine = 'https://www.figma.com/api/mcp/asset/c38c1f7f-257a-4ea1-a7ed-6f65fa8161be.svg'
const parkingIcon = 'https://www.figma.com/api/mcp/asset/00427931-c4bd-470f-8cd3-fb89e16494f3.svg'
const elevatorIcon = 'https://www.figma.com/api/mcp/asset/b15464db-12b1-4d1d-8384-d2cfa8dfd470.svg'

export function GuidanceCompletedPage() {
  const navigate = useNavigate()

  return (
    <div className="mobile-page guidance-complete-page" data-figma-node="118:8720">
      <div className="guidance-complete-spacer" aria-hidden="true" />
      <main className="guidance-complete-main">
        <section className="guidance-success">
          <img src={successIcon} alt="" />
          <h1>안내가 완료되었습니다.</h1>
        </section>

        <section className="guidance-stats" aria-label="완료 경로 요약">
          <div className="guidance-stats__numbers">
            <span>총 <strong>125</strong>M</span>
            <span><strong>3</strong>분 <strong>45</strong>초 소요</span>
          </div>

          <div className="route-summary">
            <div>
              <span><img src={gateIcon} alt="" /></span>
              <small>후문 진입</small>
            </div>
            <img className="route-summary__line" src={routeLine} alt="" />
            <div>
              <span><img src={parkingIcon} alt="" /></span>
              <small>정차 지점</small>
            </div>
            <img className="route-summary__line" src={routeLine} alt="" />
            <div>
              <span className="active"><img src={elevatorIcon} alt="" /></span>
              <small>엘리베이터</small>
            </div>
          </div>
        </section>

        <section className="guidance-complete-actions">
          <button type="button" onClick={() => navigate('/reports/record')}>현장 팁 기록</button>
          <button type="button" onClick={() => navigate('/home')}>메인으로</button>
        </section>
      </main>
    </div>
  )
}
