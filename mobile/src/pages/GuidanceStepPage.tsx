import { useNavigate } from 'react-router-dom'

const mapImage = 'https://www.figma.com/api/mcp/asset/5eec0064-266f-4b42-8218-18917f25670d.png'
const menuIcon = 'https://www.figma.com/api/mcp/asset/cc18dc51-59cc-4082-b5de-2bc9465521f9.svg'
const locationIcon = 'https://www.figma.com/api/mcp/asset/691c7a28-c7c8-49a8-8948-91a7c062add6.svg'
const plusIcon = 'https://www.figma.com/api/mcp/asset/2f0d402a-aa86-42d7-bdc7-b8d01866f6db.svg'
const minusIcon = 'https://www.figma.com/api/mcp/asset/7c013ee9-d6ef-4be6-a20f-90108753093f.svg'
const turnIcon = 'https://www.figma.com/api/mcp/asset/6c291f76-5bd6-4303-9577-7cd424704072.svg'
const endIcon = 'https://www.figma.com/api/mcp/asset/3325c95e-6f0a-4d56-aecc-265a77415cc0.svg'

export function GuidanceStepPage() {
  const navigate = useNavigate()

  return (
    <div className="mobile-page guidance-step-page" data-figma-node="118:9178">
      <header className="guidance-header guidance-header--plain">
        <button type="button" aria-label="이전 화면" onClick={() => navigate('/guidance/preview')}>
          <img src={menuIcon} alt="" />
        </button>
      </header>

      <div className="guidance-step-map">
        <img src={mapImage} alt="단계별 길안내 지도" />
      </div>

      <aside className="guidance-map-controls" aria-label="지도 컨트롤">
        <button type="button" aria-label="현재 위치">
          <img src={locationIcon} alt="" />
        </button>
        <div>
          <button type="button" aria-label="확대"><img src={plusIcon} alt="" /></button>
          <button type="button" aria-label="축소"><img src={minusIcon} alt="" /></button>
        </div>
      </aside>

      <section className="guidance-step-card" aria-label="현재 안내 단계">
        <div className="guidance-step-card__head">
          <img src={turnIcon} alt="" />
          <strong>우회전</strong>
        </div>
        <div className="guidance-step-card__body">
          <div className="guidance-step-card__meta">
            <strong>120m 앞</strong>
            <span className="guidance-step-card__divider" aria-hidden="true" />
            <strong>약 2분 후</strong>
          </div>
          <p>후문으로 차량 진입.</p>
        </div>
      </section>

      <button className="guidance-end" type="button" onClick={() => navigate('/guidance/completed')}>
        <img src={endIcon} alt="" />
        <span>안내 종료</span>
      </button>
    </div>
  )
}
